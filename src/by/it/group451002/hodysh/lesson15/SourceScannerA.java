package by.it.group451002.hodysh.lesson15;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.file.*;
import java.util.*;

public class SourceScannerA {

    public static void main(String[] args) {
        // Формируем путь к каталогу src текущего проекта
        String src = System.getProperty("user.dir")
                + File.separator + "src" + File.separator;

        Path root = Paths.get(src);
        if (!Files.exists(root)) {
            System.err.println("Каталог src не найден: " + root);
            return;
        }

        List<Result> results = new ArrayList<>();

        try (var stream = Files.walk(root)) {
            // Рекурсивно обходим все файлы в каталоге src
            stream.filter(Files::isRegularFile)               // Только файлы, не каталоги
                    .filter(p -> p.toString().endsWith(".java")) // Только Java-файлы
                    .forEach(p -> processFile(p, root, results)); // Обрабатываем каждый файл
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Сортировка: сначала по размеру, потом по пути
        // Comparator.comparingLong - сортировка по числовому полю size
        // thenComparing - дополнительная сортировка по строковому полю relPath при равенстве size
        results.sort(Comparator.comparingLong((Result r) -> r.size)
                .thenComparing(r -> r.relPath));

        // Вывод результатов в формате: размер относительный_путь
        for (Result r : results) {
            System.out.println(r.size + " " + r.relPath);
        }
    }

    // Обработка одного файла: чтение, очистка, подсчет размера
    private static void processFile(Path file, Path root, List<Result> results) {
        // Чтение файла с обработкой возможных проблем с кодировкой
        String text = readFileSafe(file, Charset.forName("UTF-8"));
        if (text == null) return; // Если не удалось прочитать, пропускаем файл

        // Удаление строк package и import (они не считаются частью логики программы)
        String processed = removePackageAndImports(text);

        // Удаление непечатаемых символов (с кодом < 33) с начала и конца текста
        // Код 32 - пробел, коды < 32 - управляющие символы
        processed = trimLowAscii(processed);

        // Вычисление размера очищенного текста в байтах (кодировка UTF-8)
        long size = processed.getBytes(Charset.forName("UTF-8")).length;

        // Получение относительного пути файла относительно корня src
        // Например: by/it/group451002/hodysh/lesson15/SourceScannerA.java
        String rel = root.relativize(file).toString();

        // Добавление результата в список
        results.add(new Result(rel, size));
    }

    // Безопасное чтение файла с обработкой исключений кодировки
    private static String readFileSafe(Path file, Charset cs) {
        try {
            // Пытаемся прочитать как UTF-8
            return Files.readString(file, cs);
        } catch (MalformedInputException e) {
            // Если UTF-8 не подходит, пробуем ISO-8859-1 (латинские символы)
            try {
                return Files.readString(file, Charset.forName("ISO-8859-1"));
            } catch (IOException ex) {
                return null; // Если и это не получилось, возвращаем null
            }
        } catch (IOException e) {
            return null; // Другие ошибки ввода-вывода
        }
    }

    // Удаление строк package и import из исходного кода
    private static String removePackageAndImports(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        try (BufferedReader br = new BufferedReader(new StringReader(text))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Убираем начальные пробелы для проверки
                String t = line.stripLeading();
                // Пропускаем строки, начинающиеся с package или import
                if (t.startsWith("package ") || t.startsWith("import ")) continue;
                // Добавляем остальные строки
                sb.append(line).append('\n');
            }
        } catch (IOException ignored) {} // Игнорируем исключение для StringReader
        return sb.toString();
    }

    // Обрезка непечатаемых символов ASCII (с кодами меньше 33) с обеих сторон строки
    // Коды 0-31: управляющие символы, 32: пробел
    private static String trimLowAscii(String s) {
        int start = 0, end = s.length() - 1;
        // Пропускаем символы с кодом < 33 в начале
        while (start <= end && s.charAt(start) < 33) start++;
        // Пропускаем символы с кодом < 33 в конце
        while (end >= start && s.charAt(end) < 33) end--;
        // Возвращаем подстроку или пустую строку, если все символы были удалены
        return start > end ? "" : s.substring(start, end + 1);
    }

    // Record (появился в Java 14) - неизменяемый класс для хранения данных
    // Автоматически создает конструктор, геттеры, equals, hashCode, toString
    private record Result(String relPath, long size) {}
}