+# Лекция 9: Spring Boot и файлы

## 1. Статика (`/static`) и загрузка файлов
**Теоретический материал:**
**Статические ресурсы** - файлы, которые отдаются клиенту без обработки сервером (CSS, JS, изображения, HTML).

**Spring Boot** автоматически настраивает обслуживание статики из папок:

- `classpath:/static`

- `classpath:/public`

- `classpath:/resources`

- `classpath:/META-INF/resources`

**Практическая настройка:**
``` properties
# application.properties
# Настройка места хранения загружаемых файлов
file.upload.dir=./uploads
# Максимальный размер файла
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# Кастомная настройка статики (если нужно)
spring.web.resources.static-locations=classpath:/static/,file:${file.upload.dir}
```
``` java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Value("${file.upload.dir}")
    private String uploadDir;
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Обслуживание загруженных файлов как статики
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
        
        // Дополнительные пути для статики
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/assets/");
    }
}
```
**Структура проекта:**
``` text
src/main/resources/
  static/
    css/style.css
    js/app.js
    images/logo.png
  templates/ (для Thymeleaf)

uploads/ (создается автоматически)
  avatars/
  documents/
```
## 2. MIME-типы и расширения файлов
**Теоретический материал:**
**MIME-тип (Multipurpose Internet Mail Extensions)** - стандарт, который указывает тип содержимого файла.

Почему изображения имеют разные расширения?

- **Разные алгоритмы сжатия:** `JPEG` использует сжатие с потерями, `PNG` - без потерь с поддержкой прозрачности

- **Разное назначение:** `GIF` поддерживает анимацию, `WebP` - современный формат от Google

- **Исторические причины:** разные компании разрабатывали форматы под свои задачи

**Основные MIME-типы изображений:**
``` java
public class ImageMimeTypes {
    public static final String JPEG = "image/jpeg";
    public static final String PNG = "image/png";
    public static final String GIF = "image/gif";
    public static final String WEBP = "image/webp";
    public static final String BMP = "image/bmp";
    public static final String SVG = "image/svg+xml";
    
    // Соответствие расширений MIME-типам
    public static final Map<String, String> EXTENSION_TO_MIME = Map.of(
        "jpg", JPEG,
        "jpeg", JPEG,
        "png", PNG,
        "gif", GIF,
        "webp", WEBP,
        "bmp", BMP,
        "svg", SVG
    );
    
    public static String getMimeTypeByExtension(String filename) {
        if (filename == null) return null;
        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex == -1) return null;
        
        String ext = filename.substring(dotIndex + 1).toLowerCase();
        return EXTENSION_TO_MIME.get(ext);
    }
}
```
**Почему нельзя доверять только расширению?**
- Пользователь может переименовать `virus.exe` в `image.jpg`

- Браузер определяет тип по MIME, а не по расширению

- Безопасность требует проверки реального содержимого

## 3. multipart/form-data
**Теоретический материал:**
**multipart/form-data** - способ кодирования данных формы, который позволяет передавать файлы вместе с обычными полями.

**Как работает:**
``` text
POST /upload HTTP/1.1
Content-Type: multipart/form-data; boundary=----WebKitFormBoundaryabc123

------WebKitFormBoundaryabc123
Content-Disposition: form-data; name="username"

John Doe
------WebKitFormBoundaryabc123
Content-Disposition: form-data; name="avatar"; filename="photo.jpg"
Content-Type: image/jpeg

[binary data of image]
------WebKitFormBoundaryabc123--
```
**HTML форма для загрузки:**
``` html
<!DOCTYPE html>
<html>
<head>
    <title>Загрузка аватарки</title>
</head>
<body>
    <form method="POST" action="/api/upload/avatar" enctype="multipart/form-data">
        <div>
            <label for="username">Имя пользователя:</label>
            <input type="text" id="username" name="username" required>
        </div>
        <div>
            <label for="avatar">Аватарка:</label>
            <input type="file" id="avatar" name="avatar" 
                   accept="image/jpeg, image/png, image/webp" required>
        </div>
        <button type="submit">Загрузить</button>
    </form>
    
    <div id="message" th:if="${message}" th:text="${message}"></div>
</body>
</html>
```

## 4. Безопасность при загрузке файлов
**Основные угрозы:**
1. Загрузка исполняемых файлов (exe, php, js)

2. Атаки переполнения буфера

3. Загрузка чрезмерно больших файлов

4. Path traversal атаки (доступ к системным файлам)

**Комплексная проверка безопасности:**
``` java
@Service
public class FileSecurityService {
    
    // Разрешенные MIME-типы
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    
    // Разрешенные расширения
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    
    // Максимальный размер файла (5MB)
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    
    // Магические числа для проверки типа файла по содержимому
    private static final Map<String, byte[]> MAGIC_NUMBERS = Map.of(
        "JPEG", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
        "PNG", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
        "GIF", new byte[]{0x47, 0x49, 0x46, 0x38},
        "WEBP", new byte[]{0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50}
    );
    
    public void validateFile(MultipartFile file) {
        // 1. Проверка размера файла
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileUploadException("Файл слишком большой. Максимальный размер: 5MB");
        }
        
        // 2. Проверка на пустой файл
        if (file.isEmpty()) {
            throw new FileUploadException("Файл пустой");
        }
        
        // 3. Проверка MIME-type из заголовка
        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new FileUploadException("Недопустимый тип файла: " + mimeType);
        }
        
        // 4. Проверка расширения файла
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.contains("..")) {
            throw new FileUploadException("Недопустимое имя файла");
        }
        
        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new FileUploadException("Недопустимое расширение файла: " + extension);
        }
        
        // 5. Проверка магических чисел (реального содержимого)
        try {
            if (!validateMagicNumbers(file.getBytes(), extension)) {
                throw new FileUploadException("Файл не соответствует заявленному формату");
            }
        } catch (IOException e) {
            throw new FileUploadException("Ошибка чтения файла");
        }
    }
    
    private boolean validateMagicNumbers(byte[] fileBytes, String extension) {
        if (fileBytes.length < 12) return false;
        
        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> checkMagicNumber(fileBytes, MAGIC_NUMBERS.get("JPEG"));
            case "png" -> checkMagicNumber(fileBytes, MAGIC_NUMBERS.get("PNG"));
            case "gif" -> checkMagicNumber(fileBytes, MAGIC_NUMBERS.get("GIF"));
            case "webp" -> checkWebPMagicNumber(fileBytes);
            default -> false;
        };
    }
    
    private boolean checkMagicNumber(byte[] fileBytes, byte[] magicNumber) {
        if (fileBytes.length < magicNumber.length) return false;
        
        for (int i = 0; i < magicNumber.length; i++) {
            if (fileBytes[i] != magicNumber[i]) {
                return false;
            }
        }
        return true;
    }
    
    private boolean checkWebPMagicNumber(byte[] fileBytes) {
        // WEBP: RIFFxxxxWEBP
        return fileBytes[0] == 0x52 && fileBytes[1] == 0x49 && 
               fileBytes[2] == 0x46 && fileBytes[3] == 0x46 &&
               fileBytes[8] == 0x57 && fileBytes[9] == 0x45 &&
               fileBytes[10] == 0x42 && fileBytes[11] == 0x50;
    }
    
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) return "";
        return filename.substring(lastDotIndex + 1);
    }
}
```

## 5. Загрузка аватарки с полной валидацией
**Полный пример реализации:**
1. DTO для запроса:
``` java
public class AvatarUploadRequest {
    @NotBlank(message = "Имя пользователя обязательно")
    @Size(min = 3, max = 50, message = "Имя должно быть от 3 до 50 символов")
    private String username;
    
    @NotNull(message = "Файл аватарки обязателен")
    private MultipartFile avatar;

    // геттеры и сеттеры
}
```
2. Сервис для работы с аватарками:
``` java
@Service
@Slf4j
public class AvatarService {
    
    @Value("${file.upload.dir}")
    private String uploadDir;
    
    @Autowired
    private FileSecurityService fileSecurityService;
    
    public AvatarUploadResponse uploadAvatar(AvatarUploadRequest request) {
        try {
            MultipartFile avatarFile = request.getAvatar();
            
            // Комплексная проверка безопасности
            fileSecurityService.validateFile(avatarFile);
            
            // Дополнительная проверка - только квадратные изображения
            validateImageDimensions(avatarFile);
            
            // Генерация безопасного имени файла
            String safeFileName = generateSafeFileName(avatarFile);
            Path uploadPath = Paths.get(uploadDir, "avatars");
            
            // Создание директории, если не существует
            Files.createDirectories(uploadPath);
            
            // Сохранение файла
            Path filePath = uploadPath.resolve(safeFileName);
            avatarFile.transferTo(filePath.toFile());
            
            // Создание миниатюры
            String thumbnailName = createThumbnail(filePath, safeFileName);
            
            log.info("Аватар успешно загружен для пользователя: {}", request.getUsername());
            
            return new AvatarUploadResponse(
                true,
                "Аватар успешно загружен",
                "/uploads/avatars/" + safeFileName,
                "/uploads/avatars/" + thumbnailName
            );
            
        } catch (FileUploadException e) {
            log.warn("Ошибка загрузки аватара: {}", e.getMessage());
            return new AvatarUploadResponse(false, e.getMessage(), null, null);
        } catch (Exception e) {
            log.error("Неожиданная ошибка при загрузке аватара", e);
            return new AvatarUploadResponse(false, "Внутренняя ошибка сервера", null, null);
        }
    }
    
    private void validateImageDimensions(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream()) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                throw new FileUploadException("Файл не является корректным изображением");
            }
            
            int width = image.getWidth();
            int height = image.getHeight();
            
            // Проверка на квадратное изображение (допуск 10%)
            double ratio = (double) width / height;
            if (ratio < 0.9 || ratio > 1.1) {
                throw new FileUploadException("Аватар должен быть квадратным");
            }
            
            // Проверка минимального и максимального размера
            if (width < 100 || height < 100) {
                throw new FileUploadException("Минимальный размер аватара: 100x100 пикселей");
            }
            
            if (width > 2000 || height > 2000) {
                throw new FileUploadException("Максимальный размер аватара: 2000x2000 пикселей");
            }
        }
    }
    
    private String generateSafeFileName(MultipartFile file) {
        String extension = getFileExtension(file.getOriginalFilename());
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = UUID.randomUUID().toString().substring(0, 8);
        
        return "avatar_" + timestamp + "_" + random + "." + extension;
    }
    
    private String createThumbnail(Path originalImagePath, String originalFileName) throws IOException {
        String extension = getFileExtension(originalFileName);
        String thumbnailName = "thumb_" + originalFileName;
        Path thumbnailPath = originalImagePath.getParent().resolve(thumbnailName);
        
        BufferedImage originalImage = ImageIO.read(originalImagePath.toFile());
        BufferedImage thumbnail = resizeImage(originalImage, 100, 100);
        
        ImageIO.write(thumbnail, extension, thumbnailPath.toFile());
        
        return thumbnailName;
    }
    
    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        graphics2D.dispose();
        
        return resizedImage;
    }
    
    private String getFileExtension(String filename) {
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
```
3. REST контроллер:
``` java
@RestController
@RequestMapping("/api/upload")
@Slf4j
public class AvatarUploadController {
    
    @Autowired
    private AvatarService avatarService;
    
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AvatarUploadResponse> uploadAvatar(
            @RequestParam("username") String username,
            @RequestParam("avatar") MultipartFile avatar) {
        
        AvatarUploadRequest request = new AvatarUploadRequest();
        request.setUsername(username);
        request.setAvatar(avatar);
        
        // Валидация базовых ограничений
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new AvatarUploadResponse(false, "Имя пользователя обязательно", null, null));
        }
        
        AvatarUploadResponse response = avatarService.uploadAvatar(request);
        
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
    
    // Альтернативный вариант с @ModelAttribute
    @PostMapping(value = "/avatar-v2", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AvatarUploadResponse> uploadAvatarV2(
            @Valid @ModelAttribute AvatarUploadRequest request) {
        
        AvatarUploadResponse response = avatarService.uploadAvatar(request);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
    
    @GetMapping("/avatar/{filename}")
    public ResponseEntity<Resource> serveAvatar(@PathVariable String filename) {
        try {
            Path filePath = Paths.get("./uploads/avatars").resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                String mimeType = Files.probeContentType(filePath);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(mimeType))
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}

// DTO для ответа
public class AvatarUploadResponse {
    private boolean success;
    private String message;
    private String avatarUrl;
    private String thumbnailUrl;
    
    // конструкторы, геттеры, сеттеры
}
```
4. Глобальный обработчик исключений:
``` java
@ControllerAdvice
public class FileUploadExceptionHandler {
    
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<AvatarUploadResponse> handleMaxSizeException() {
        return ResponseEntity.badRequest()
                .body(new AvatarUploadResponse(false, "Файл слишком большой", null, null));
    }
    
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<AvatarUploadResponse> handleMultipartException() {
        return ResponseEntity.badRequest()
                .body(new AvatarUploadResponse(false, "Ошибка при загрузке файла", null, null));
    }
}
```
**Ключевые моменты безопасности:**
1. Проверка MIME-type на трех уровнях: заголовок, расширение, магические числа
2. Валидация размеров файла и изображения
3. Генерация безопасных имен файлов для предотвращения path traversal
4. Ограничение типов файлов только изображениями
5. Создание миниатюр для уменьшения нагрузки
6. Правильная обработка исключений

Этот подход обеспечивает безопасную загрузку файлов с комплексной валидацией на всех уровнях.