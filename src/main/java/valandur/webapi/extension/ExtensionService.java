package valandur.webapi.extension;

import org.slf4j.Logger;
import valandur.webapi.WebAPI;
import valandur.webapi.api.extension.IExtensionService;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ExtensionService implements IExtensionService {

    private Map<String, String> relocatedPackages = new HashMap<>();


    public void init() {
        // Relocated packages
        relocatedPackages.clear();
        relocatedPackages.put("import io.sentry",       "import valandur.webapi.shadow.io.sentry");
        relocatedPackages.put("import org.eclipse",     "import valandur.webapi.shadow.org.eclipse");
        relocatedPackages.put("import com.fasterxml",   "import valandur.webapi.shadow.fasterxml");
        relocatedPackages.put("import javax.servlet",   "import valandur.webapi.shadow.javax.servlet");
        relocatedPackages.put("import net.jodah",       "import valandur.webapi.shadow.net.jodah");
        relocatedPackages.put("import org.mindrot",     "import valandur.webapi.shadow.org.mindrot");
        relocatedPackages.put("import edu.umdh",        "import valandur.webapi.shadow.edu.umd");
        relocatedPackages.put("import javassist",       "import valandur.webapi.shadow.javassist");
        relocatedPackages.put("import net.jcip",        "import valandur.webapi.shadow.net.jcip");
    }

    public <T> void loadPlugins(String pkg, Class<T> baseClass, Consumer<Class<T>> done) {
        Logger logger = WebAPI.getLogger();

        File folder = new File("webapi/" + pkg);
        if (!folder.exists() && !folder.mkdirs()) {
            logger.error("Could not create folder " + folder.getAbsolutePath());
            return;
        }

        List<File> files = null;
        try {
            files = Files.walk(Paths.get(folder.toURI()))
                    .filter(Files::isRegularFile)
                    .filter(f -> f.toString().endsWith(".java"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            if (WebAPI.reportErrors()) WebAPI.sentryCapture(e);
            return;
        }

        logger.info("Found " + files.size() + " files in " + folder.getAbsolutePath());
        if (files.size() == 0) {
            return;
        }

        // Setup java compiler
        ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
        URL[] urls = ((URLClassLoader) currentCl).getURLs();
        String classpath = Arrays.stream(urls).map(URL::getPath).filter(Objects::nonNull).collect(Collectors.joining(File.pathSeparator));

        DiagnosticListener diag = new DiagnosticListener();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            logger.error("You need to install a JDK to use extensions");
            return;
        }

        StandardJavaFileManager fm = compiler.getStandardFileManager(diag, null, null);
        List<String> optionList = Arrays.asList("-classpath", classpath);

        // Compile, load and instantiate compiled class.
        for (File file : files) {
            String logFile = file.getAbsolutePath().replace(".java", ".log");
            diag.startLog(logFile);

            try {
                logger.info("  - " + file.getName());

                // Read file to check for some basic things like package and shadowed references
                String fileContent = new String(Files.readAllBytes(file.toPath()));

                if (!fileContent.contains("package " + pkg)) {
                    logger.error("   The class must be in the '" + pkg + "' package.");
                    continue;
                }

                int start = fileContent.indexOf("class ") + 6;
                int end = fileContent.indexOf(" ", start);
                String cName = fileContent.substring(start, end);
                if (!cName.equalsIgnoreCase(file.getName().substring(0, file.getName().length() - 5))) {
                    logger.error("   File name '" + file.getName().substring(0, file.getName().length() - 5) + "' must match class name '" + cName + "'");
                    continue;
                }

                if (!fileContent.contains("extends " + baseClass.getSimpleName())) {
                    logger.error("   Class must extend '" + baseClass.getSimpleName() + "'");
                    continue;
                }

                // Replace shadowed references
                for (Map.Entry<String, String> entry : relocatedPackages.entrySet()) {
                    if (WebAPI.isDevMode())
                        fileContent = fileContent.replace(entry.getValue(), entry.getKey());
                    else
                        fileContent = fileContent.replace(entry.getKey(), entry.getValue());
                }

                // Write back to file
                Files.write(file.toPath(), fileContent.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                // Compile the file
                Iterable<? extends JavaFileObject> compilationUnits = fm.getJavaFileObjectsFromFiles(Collections.singletonList(file));
                JavaCompiler.CompilationTask task = compiler.getTask(null, fm, diag, optionList, null, compilationUnits);

                boolean res = task.call();

                if (!res) {
                    logger.error("   Compilation failed. See the log file at " + logFile + " for details");
                    continue;
                }

                String className = file.getName().substring(0, file.getName().length() - 5);

                // Load the class
                URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{folder.getParentFile().toURI().toURL()}, currentCl);
                Class<?> cls = Class.forName(pkg + "." + className, true, classLoader);
                if (!baseClass.isAssignableFrom(cls)) {
                    logger.error("   Must extend " + baseClass.getName());
                    continue;
                }

                // Do whatever we want with the successfully loaded extension
                done.accept((Class<T>) cls);

                logger.info("    -> " + cls.getName());
            } catch (IOException | ClassNotFoundException e) {
                logger.error("   Error: See the log file at " + logFile + " for details");
                if (WebAPI.reportErrors()) WebAPI.sentryCapture(e);
                diag.writeException(e);
            }

            diag.stopLog();
        }

        diag.stopLog();
    }
}
