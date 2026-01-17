package hr.ipicek.jamb.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


public class DocumentationGenerator {

    private static final String OUTPUT_DIR = "documentation";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    public static void generateDocumentation(String basePackage) {
        try {
            // Kreiraj output direktorij
            File outputDir = new File(OUTPUT_DIR);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            // Pronađi sve klase u package-u
            List<Class<?>> classes = findAllClasses(basePackage);

            System.out.println("Pronađeno " + classes.size() + " klasa za dokumentiranje...");

            // Generiraj dokumentaciju za svaku klasu
            for (Class<?> clazz : classes) {
                generateClassDocumentation(clazz);
            }

            // Generiraj index stranicu
            generateIndexPage(classes);

            System.out.println("Dokumentacija generirana u: " + outputDir.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("Greška pri generiranju dokumentacije: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private static List<Class<?>> findAllClasses(String basePackage) {
        List<Class<?>> classes = new ArrayList<>();

        try {
            // Hardcoded lista glavnih package-ova za skeniranje
            String[] packages = {
                    "hr.ipicek.jamb.model",
                    "hr.ipicek.jamb.controller",
                    "hr.ipicek.jamb.network",
                    "hr.ipicek.jamb.network.rmi",
                    "hr.ipicek.jamb.network.socket",
                    "hr.ipicek.jamb.network.protocol",
                    "hr.ipicek.jamb.util",
                    "hr.ipicek.jamb.logging"
            };

            for (String pkg : packages) {
                classes.addAll(getClassesInPackage(pkg));
            }

        } catch (Exception e) {
            System.err.println("Greška pri skeniranju klasa: " + e.getMessage());
        }

        return classes;
    }


    private static List<Class<?>> getClassesInPackage(String packageName) {
        List<Class<?>> classes = new ArrayList<>();

        // Poznate klase - hardcoded za demonstraciju Reflection API-ja
        String[] classNames = getKnownClassNames(packageName);

        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(packageName + "." + className);
                classes.add(clazz);
            } catch (ClassNotFoundException e) {
                // Skip classes that can't be loaded
            }
        }

        return classes;
    }


    private static String[] getKnownClassNames(String packageName) {
        return switch (packageName) {
            case "hr.ipicek.jamb.model" -> new String[]{
                    "GameEngine", "Player", "ScoreSheet", "DiceSet", "Die", "ScoreCategory", "Move"
            };
            case "hr.ipicek.jamb.controller" -> new String[]{
                    "MainController", "MenuController", "NetworkGameController",
                    "NetworkLobbyController", "HostGameController", "JoinGameController", "SettingsController"
            };
            case "hr.ipicek.jamb.network" -> new String[]{
                    "NetworkGameEngine", "NetworkGameManager"
            };
            case "hr.ipicek.jamb.network.rmi" -> new String[]{
                    "RMIClient", "JNDIClient", "RMIRegistryServer", "LobbyService", "ChatService"
            };
            case "hr.ipicek.jamb.network.socket" -> new String[]{
                    "GameServer", "GameClient", "ClientHandler"
            };
            case "hr.ipicek.jamb.network.protocol" -> new String[]{
                    "GameMessage", "GameStateUpdate", "MessageType"
            };
            case "hr.ipicek.jamb.util" -> new String[]{
                    "DialogUtils", "SceneUtils", "ViewPaths", "Logger", "NetworkConstants", "ErrorMessages"
            };
            case "hr.ipicek.jamb.logging" -> new String[]{
                    "MoveLogger", "MoveDisplay"
            };
            default -> new String[]{};
        };
    }

    /**
     * Generira HTML dokumentaciju za jednu klasu koristeći Reflection API
     */
    private static void generateClassDocumentation(Class<?> clazz) throws IOException {
        StringBuilder html = new StringBuilder();

        // HTML Header
        html.append("<!DOCTYPE html>\n<html>\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<title>").append(clazz.getSimpleName()).append(" - Documentation</title>\n");
        html.append("<style>\n");
        html.append(getCSS());
        html.append("</style>\n");
        html.append("</head>\n<body>\n");

        // Class Header
        html.append("<div class=\"container\">\n");
        html.append("<h1>").append(clazz.getSimpleName()).append("</h1>\n");
        html.append("<p class=\"package\">Package: ").append(clazz.getPackage().getName()).append("</p>\n");

        // Class modifiers using Reflection
        int modifiers = clazz.getModifiers();
        html.append("<p class=\"modifiers\">");
        if (Modifier.isPublic(modifiers)) html.append("public ");
        if (Modifier.isAbstract(modifiers)) html.append("abstract ");
        if (Modifier.isFinal(modifiers)) html.append("final ");
        if (clazz.isInterface()) html.append("interface ");
        else html.append("class ");
        html.append("</p>\n");

        // Superclass using Reflection
        if (clazz.getSuperclass() != null && !clazz.getSuperclass().equals(Object.class)) {
            html.append("<p>Extends: ").append(clazz.getSuperclass().getSimpleName()).append("</p>\n");
        }

        // Interfaces using Reflection
        Class<?>[] interfaces = clazz.getInterfaces();
        if (interfaces.length > 0) {
            html.append("<p>Implements: ");
            html.append(Arrays.stream(interfaces)
                    .map(Class::getSimpleName)
                    .collect(Collectors.joining(", ")));
            html.append("</p>\n");
        }

        // Annotations using Reflection
        Annotation[] annotations = clazz.getAnnotations();
        if (annotations.length > 0) {
            html.append("<h2>Annotations</h2>\n<ul>\n");
            for (Annotation ann : annotations) {
                html.append("<li>@").append(ann.annotationType().getSimpleName()).append("</li>\n");
            }
            html.append("</ul>\n");
        }

        // Fields using Reflection
        html.append("<h2>Fields (").append(clazz.getDeclaredFields().length).append(")</h2>\n");
        html.append("<table>\n");
        html.append("<tr><th>Modifiers</th><th>Type</th><th>Name</th></tr>\n");

        for (Field field : clazz.getDeclaredFields()) {
            html.append("<tr>");
            html.append("<td>").append(Modifier.toString(field.getModifiers())).append("</td>");
            html.append("<td>").append(field.getType().getSimpleName()).append("</td>");
            html.append("<td>").append(field.getName()).append("</td>");
            html.append("</tr>\n");
        }
        html.append("</table>\n");

        // Constructors using Reflection
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        if (constructors.length > 0) {
            html.append("<h2>Constructors (").append(constructors.length).append(")</h2>\n");
            html.append("<table>\n");
            html.append("<tr><th>Modifiers</th><th>Parameters</th></tr>\n");

            for (Constructor<?> constructor : constructors) {
                html.append("<tr>");
                html.append("<td>").append(Modifier.toString(constructor.getModifiers())).append("</td>");
                html.append("<td>").append(getParameterString(constructor.getParameterTypes())).append("</td>");
                html.append("</tr>\n");
            }
            html.append("</table>\n");
        }

        // Methods using Reflection
        html.append("<h2>Methods (").append(clazz.getDeclaredMethods().length).append(")</h2>\n");
        html.append("<table>\n");
        html.append("<tr><th>Modifiers</th><th>Return Type</th><th>Name</th><th>Parameters</th></tr>\n");

        for (Method method : clazz.getDeclaredMethods()) {
            html.append("<tr>");
            html.append("<td>").append(Modifier.toString(method.getModifiers())).append("</td>");
            html.append("<td>").append(method.getReturnType().getSimpleName()).append("</td>");
            html.append("<td><strong>").append(method.getName()).append("</strong></td>");
            html.append("<td>").append(getParameterString(method.getParameterTypes())).append("</td>");
            html.append("</tr>\n");
        }
        html.append("</table>\n");

        // Footer
        html.append("<p class=\"footer\">Generated: ").append(LocalDateTime.now().format(DATE_FORMAT)).append("</p>\n");
        html.append("</div>\n</body>\n</html>");

        // Write to file
        File outputFile = new File(OUTPUT_DIR, clazz.getSimpleName() + ".html");
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(html.toString());
        }

        System.out.println("✓ Generirana dokumentacija: " + clazz.getSimpleName());
    }

    /**
     * Generira index stranicu sa svim klasama
     */
    private static void generateIndexPage(List<Class<?>> classes) throws IOException {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n<html>\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<title>Jamb Game - API Documentation</title>\n");
        html.append("<style>\n").append(getCSS()).append("</style>\n");
        html.append("</head>\n<body>\n");
        html.append("<div class=\"container\">\n");
        html.append("<h1>Jamb Game - API Documentation</h1>\n");
        html.append("<p>Generated using Java Reflection API</p>\n");
        html.append("<p class=\"package\">Total Classes: ").append(classes.size()).append("</p>\n");

        // Group by package
        Map<String, List<Class<?>>> byPackage = classes.stream()
                .collect(Collectors.groupingBy(c -> c.getPackage().getName()));

        for (Map.Entry<String, List<Class<?>>> entry : byPackage.entrySet()) {
            html.append("<h2>").append(entry.getKey()).append("</h2>\n");
            html.append("<ul>\n");

            for (Class<?> clazz : entry.getValue()) {
                html.append("<li><a href=\"").append(clazz.getSimpleName()).append(".html\">");
                html.append(clazz.getSimpleName()).append("</a>");

                // Add class type
                if (clazz.isInterface()) html.append(" <span class=\"tag\">(interface)</span>");
                if (clazz.isEnum()) html.append(" <span class=\"tag\">(enum)</span>");
                if (Modifier.isAbstract(clazz.getModifiers())) html.append(" <span class=\"tag\">(abstract)</span>");

                html.append("</li>\n");
            }

            html.append("</ul>\n");
        }

        html.append("<p class=\"footer\">Generated: ").append(LocalDateTime.now().format(DATE_FORMAT)).append("</p>\n");
        html.append("</div>\n</body>\n</html>");

        File indexFile = new File(OUTPUT_DIR, "index.html");
        try (FileWriter writer = new FileWriter(indexFile)) {
            writer.write(html.toString());
        }

        System.out.println("✓ Generirana index stranica");
    }

    /**
     * Helper metoda za formatiranje parametara
     */
    private static String getParameterString(Class<?>[] params) {
        if (params.length == 0) return "()";

        return "(" + Arrays.stream(params)
                .map(Class::getSimpleName)
                .collect(Collectors.joining(", ")) + ")";
    }

    /**
     * CSS stilovi za HTML dokumentaciju
     */
    private static String getCSS() {
        return """
            body {
                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                background: #f5f5f5;
                margin: 0;
                padding: 20px;
            }
            .container {
                max-width: 1200px;
                margin: 0 auto;
                background: white;
                padding: 30px;
                border-radius: 8px;
                box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            }
            h1 {
                color: #2c3e50;
                border-bottom: 3px solid #3498db;
                padding-bottom: 10px;
            }
            h2 {
                color: #34495e;
                margin-top: 30px;
                border-bottom: 2px solid #ecf0f1;
                padding-bottom: 5px;
            }
            .package {
                color: #7f8c8d;
                font-size: 14px;
            }
            .modifiers {
                color: #e74c3c;
                font-weight: bold;
            }
            table {
                width: 100%;
                border-collapse: collapse;
                margin: 15px 0;
            }
            th {
                background: #3498db;
                color: white;
                padding: 12px;
                text-align: left;
            }
            td {
                padding: 10px;
                border-bottom: 1px solid #ecf0f1;
            }
            tr:hover {
                background: #f8f9fa;
            }
            .footer {
                margin-top: 40px;
                padding-top: 20px;
                border-top: 1px solid #ecf0f1;
                color: #95a5a6;
                font-size: 12px;
                text-align: center;
            }
            ul {
                list-style: none;
                padding: 0;
            }
            li {
                padding: 8px;
                border-bottom: 1px solid #ecf0f1;
            }
            a {
                color: #3498db;
                text-decoration: none;
                font-weight: 500;
            }
            a:hover {
                text-decoration: underline;
            }
            .tag {
                background: #e74c3c;
                color: white;
                padding: 2px 8px;
                border-radius: 3px;
                font-size: 11px;
                margin-left: 5px;
            }
            """;
    }


    public static void main(String[] args) {
        System.out.println("=== Reflection API Documentation Generator ===");
        System.out.println("Generating documentation...\n");

        generateDocumentation("hr.ipicek.jamb");

        System.out.println("\n=== Done! ===");
        System.out.println("Open 'documentation/index.html' to view the documentation.");
    }
}