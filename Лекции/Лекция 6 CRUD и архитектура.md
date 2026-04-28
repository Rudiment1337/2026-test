# Лекция 6: CRUD и архитектура
##  1. CRUD (Create, Read, Update, Delete)
Теоретический материал:
**CRUD** — это четыре базовые операции, которые вы выполняете с данными в любом приложении:

- **Create (Создание)**: Добавление новой записи (например, создание нового пользователя).

- **Read (Чтение)**: Получение записей (одной или списка).

- **Update (Обновление)**: Изменение существующей записи.

- **Delete (Удаление)**: Удаление записи.

В Spring Boot эти операции реализуются через связку REST Controller и Repository.

Ключевые компоненты Spring:

- **Сущность (Entity): Обычный Java-класс (POJO)**, который представляет таблицу в базе данных.

- **Репозиторий (Repository): Интерфейс**, который наследует `JpaRepository`. Spring Boot автоматически создает реализацию этого интерфейса со всеми стандартными CRUD-методами.

- **Сервис (Service): Класс**, содержащий бизнес-логику. Он использует репозиторий для доступа к данным.

- **REST Контроллер (Controller): Класс**, который обрабатывает HTTP-запросы (GET, POST, PUT, DELETE) и возвращает ответы.

Практический пример (Код):

Допустим, у нас есть сущность `User`.

``` java
// 1. Сущность (Entity)
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
    // Конструкторы, геттеры и сеттеры ОБЯЗАТЕЛЬНЫ
}

// 2. Репозиторий (Repository)
// Наследуемся от JpaRepository<Тип_Сущности, Тип_ID>
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Spring Data JPA автоматически предоставит методы:
    // save(), findById(), findAll(), deleteById() и т.д.
}

// 3. Сервис (Service) - опциональный, но рекомендуемый слой
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public User create(User user) {
        return userRepository.save(user);
    }

    public List<User> readAll() {
        return userRepository.findAll();
    }

    public User readById(Long id) {
        return userRepository.findById(id).orElseThrow();
    }

    public User update(User newUserData, Long id) {
        User existingUser = readById(id);
        existingUser.setName(newUserData.getName());
        existingUser.setEmail(newUserData.getEmail());
        return userRepository.save(existingUser);
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }
}

// 4. REST Контроллер (Controller)
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // CREATE (POST)
    @PostMapping
    public User create(@RequestBody User user) {
        return userService.create(user);
    }

    // READ (GET) - все
    @GetMapping
    public List<User> readAll() {
        return userService.readAll();
    }

    // READ (GET) - по ID
    @GetMapping("/{id}")
    public User readById(@PathVariable Long id) {
        return userService.readById(id);
    }

    // UPDATE (PUT)
    @PutMapping("/{id}")
    public User update(@RequestBody User user, @PathVariable Long id) {
        return userService.update(user, id);
    }

    // DELETE (DELETE)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
```

## 2. Validation (Валидация)
**Теоретический материал:**
Валидация — это проверка данных, приходящих от клиента (например, из формы), на соответствие определенным правилам до того, как эти данные будут обработаны. Это критически важно для безопасности и целостности данных.

**Как это работает в Spring:**
Вы помечаете поля сущности специальными аннотациями. Затем в контроллере перед методом вы ставите аннотацию `@Valid`, которая говорит Spring: "Проверь этот объект по правилам".

**Основные аннотации валидации (из пакета `jakarta.validation.constraints`):**

- `@NotNull` — поле не должно быть `null`.

- `@NotBlank` — строка не должна быть `null` и должна содержать хотя бы один непробельный символ.

- `@Size(min=, max=)` — ограничение длины строки или коллекции.

- `@Email` — проверка формата email.

- `@Min(value)` / `@Max(value)` — ограничение для числовых значений.

**Практический пример (Дополнение к CRUD):**
``` java
// 1. Обновляем сущность User, добавляя аннотации валидации
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Имя не может быть пустым") // Валидация
    private String name;

    @Email(message = "Некорректный формат email") // Валидация
    @NotBlank(message = "Email не может быть пустым")
    private String email;
    // ... геттеры и сеттеры
}

// 2. Обновляем метод контроллера (добавляем @Valid)
@RestController
@RequestMapping("/api/users")
public class UserController {

    @PostMapping
    // Ключевой момент: добавляем @Valid перед @RequestBody
    public User create(@Valid @RequestBody User user) {
        return userService.create(user);
    }

    @PutMapping("/{id}")
    public User update(@Valid @RequestBody User user, @PathVariable Long id) {
        return userService.update(user, id);
    }
}
```
**Что происходит, если валидация не пройдена?**
Spring выбросит исключение `MethodArgumentNotValidException`. Чтобы обработать его и вернуть клиенту красивую ошибку, используется `@ExceptionHandler`.

``` java
// Обработка ошибок валидации (обычно внутри @RestControllerAdvice или самого контроллера)
@ResponseStatus(HttpStatus.BAD_REQUEST) // Код ответа 400
@ExceptionHandler(MethodArgumentNotValidException.class)
public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach((error) -> {
        String fieldName = ((FieldError) error).getField();
        String errorMessage = error.getDefaultMessage();
        errors.put(fieldName, errorMessage);
    });
    return errors; // Вернет JSON вида { "name": "Имя не может быть пустым" }
}
```

## 3. DAO (Data Access Object)
**Теоретический материал:**
`DAO` — это паттерн (шаблон проектирования), который разделяет код бизнес-логики и код для работы с базой данных.

- **Цель:** Создать абстракцию над источником данных. Ваш сервис не должен знать, как именно сохраняются данные (в `MySQL`, `PostgreSQL`, в файл или в память). Он просто работает с интерфейсом `DAO` (например, `UserDao`).

- **Классический подход:** Вы создаете интерфейс `UserDao` с методами `findById`, `save`, `delete` и т.д., а затем пишете его реализацию, используя, например, JDBC (`JdbcTemplate`).

**Важный момент:**
В современном Spring Boot паттерн **Repository** (который мы использовали в разделе CRUD, например, `JpaRepository`) является эволюцией паттерна DAO. `JpaRepository` — это, по сути, готовый, стандартизированный DAO. Поэтому чаще всего вы будете использовать именно его.

**Когда может понадобиться свой DAO?**
Когда нужны сложные, нестандартные запросы, которые не покрываются стандартными методами `JpaRepository`.

**Практический пример (классический DAO с `JdbcTemplate`):**

``` java 
// 1. Интерфейс DAO
public interface UserDao {
    User findById(Long id);
    List<User> findAll();
    void save(User user);
    void update(User user);
    void delete(Long id);
}

// 2. Реализация DAO
@Repository
public class UserDaoImpl implements UserDao {

    @Autowired
    private JdbcTemplate jdbcTemplate; // Помощник для работы с JDBC

    @Override
    public User findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        // Выполняем запрос и "маппим" результат на Java-объект User
        return jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(User.class), id);
    }

    @Override
    public void save(User user) {
        String sql = "INSERT INTO users (name, email) VALUES (?, ?)";
        jdbcTemplate.update(sql, user.getName(), user.getEmail());
    }
    // ... остальные методы
}

// 3. Сервис будет использовать UserDao, а не UserRepository
@Service
public class UserService {
    @Autowired
    private UserDao userDao; // Работаем через абстракцию

    public User findById(Long id) {
        return userDao.findById(id);
    }
}
```

## 4. REST (Representational State Transfer)
**Теоретический материал:**
REST — это архитектурный стиль для построения веб-сервисов. Главные принципы:

1. **Ресурсы:** Все является ресурсом (например, Пользователь, Книга). Каждый ресурс имеет уникальный URL (например, `/api/users`).

2. **HTTP-методы:** Действия с ресурсами определяются HTTP-методами:

- `GET /api/users` — получить список всех пользователей.

- `GET /api/users/1` — получить пользователя с ID=1.

- `POST /api/users `— создать нового пользователя. Данные передаются в теле запроса (JSON).

- `PUT /api/users/1` — обновить ВСЕ данные пользователя с ID=1.

- `PATCH /api/users/1` — обновить ЧАСТЬ данных пользователя.

- `DELETE /api/users/1 `— удалить пользователя с ID=1.

3. **Stateless (Без состояния):** Сервер не хранит состояние клиента между запросами.

4. **Представление данных:** Чаще всего данные передаются в формате JSON.

**Практический пример:**
Весь код из раздела 1 (**CRUD**) — это и есть реализация REST-сервиса. Обратите внимание на соответствие URL и HTTP-методов.

**Ключевые аннотации Spring для REST:**

- `@RestController` — помечает класс как контроллер, который возвращает данные (JSON/XML), а не имя представления (HTML-страницы).

- `@RequestMapping` — задает корневой URL для всех методов контроллера.

- `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` — задают конкретный URL и HTTP-метод для каждого метода.

- `@PathVariable` — извлекает значение из URL (например, `{id}`).

- `@RequestBody` — преобразует JSON из тела запроса в Java-объект.