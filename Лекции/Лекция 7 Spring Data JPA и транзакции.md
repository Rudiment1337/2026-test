# Лекция 7: Spring Data JPA и транзакции

## 1. Spring Data JPA и транзакции
**Теоретический материал:**
**Spring Data JPA** — это надстройка над JPA (Hibernate), которая значительно упрощает работу с базами данных. Она автоматически создает реализации репозиториев, уменьшая объем шаблонного кода.

**Транзакции** — это способ группировки нескольких операций с базой данных в одну логическую единицу работы. Основное правило: "**Все или ничего**" — либо выполняются все операции транзакции, либо ни одной.

**Ключевые аннотации:**
- `@Transactional` — указывает, что метод должен выполняться в транзакции

- `@EnableTransactionManagement` — включает поддержку транзакций (в Spring Boot добавляется автоматически)

**Практический пример:**

``` java
@Service
public class BankService {
    
    @Autowired
    private AccountRepository accountRepository;
    
    // Этот метод будет выполняться в одной транзакции
    @Transactional
    public void transferMoney(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        // 1. Находим оба счета
        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new AccountNotFoundException());
        Account toAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new AccountNotFoundException());
        
        // 2. Проверяем достаточно ли денег
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }
        
        // 3. Списываем деньги с одного счета
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        accountRepository.save(fromAccount);
        
        // 4. Зачисляем деньги на другой счет
        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountRepository.save(toAccount);
        
        // Если здесь произойдет исключение - ВСЕ изменения откатятся!
    }
}
```
## 2. JpaRepository
**Теоретический материал:**
JpaRepository — это интерфейс из Spring Data JPA, который предоставляет готовые методы для работы с сущностями. Он наследует от `CrudRepository` и добавляет JPA-специфичные функции.

**Основные возможности:**
- Стандартные CRUD-методы: `save()`, `findById()`, `findAll()`, `delete()`, `count()`

- Пагинация и сортировка: `findAll(Pageable)`, `findAll(Sort)`

- Пакетные операции: `saveAll()`, `deleteAll()`

- Методы на основе именования: Spring автоматически реализует методы по их имени

**Практический пример:**

``` java
// Наследуемся от JpaRepository<ТипСущности, ТипId>
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Spring Data JPA автоматически создаст реализацию!
    
    // Стандартные методы уже есть:
    // List<User> findAll();
    // Optional<User> findById(Long id);
    // User save(User user);
    // void delete(User user);
    
    // Автогенерация методов по имени:
    List<User> findByName(String name); // SELECT * FROM users WHERE name = ?
    List<User> findByNameContainingIgnoreCase(String name); // WHERE name ILIKE '%name%'
    List<User> findByEmailEndingWith(String domain); // WHERE email LIKE '%@domain'
    
    // Сортировка и пагинация
    List<User> findAllByOrderByNameAsc();
    Page<User> findByActiveTrue(Pageable pageable);
    
    // Счетчики
    long countByActiveTrue();
    
    // Удаление по условию
    void deleteByActiveFalse();
    
    // Нативные запросы
    @Query("SELECT u FROM User u WHERE u.createdDate > :date")
    List<User> findUsersCreatedAfter(@Param("date") LocalDateTime date);
    
    @Query(value = "SELECT * FROM users u WHERE u.age > :minAge", nativeQuery = true)
    List<User> findUsersOlderThan(@Param("minAge") int minAge);
}
```

## 3. @Entity
**Теоретический материал:**
**@Entity** — это аннотация JPA, которая указывает, что класс является сущностью и должен быть отображен на таблицу в базе данных.

**Ключевые аннотации для сущностей:**
- `@Entity` — помечает класс как сущность

- `@Table(name = "table_name")` — указывает имя таблицы

- `@Id` — помечает поле как первичный ключ

- `@GeneratedValue` — определяет стратегию генерации ID

- `@Column(name = "column_name")` — маппинг поля на столбец

- `@OneToMany`, `@ManyToOne`, `@ManyToMany` — связи между сущностями

**Практический пример:**
``` java
@Entity
@Table(name = "users") // если имя таблицы отличается от имени класса
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // автоинкремент
    private Long id;
    
    @Column(name = "user_name", nullable = false, length = 100)
    private String name;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    private Integer age;
    
    @Enumerated(EnumType.STRING) // хранить enum как строку
    private UserStatus status;
    
    @CreationTimestamp
    private LocalDateTime createdDate;
    
    @UpdateTimestamp
    private LocalDateTime lastModifiedDate;
    
    // Связь один-ко-многим (один пользователь - много заказов)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Order> orders = new ArrayList<>();
    
    // Конструкторы, геттеры, сеттеры
}

// Пример связи между сущностями
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue
    private Long id;
    
    private BigDecimal amount;
    
    @ManyToOne(fetch = FetchType.LAZY) // ленивая загрузка
    @JoinColumn(name = "user_id")
    private User user;
}
```

## 4. @Repository
**Теоретический материал:**
**@Repository** — это специализированный вариант `@Component`, который помечает класс как репозиторий (DAO). Его основные функции:

1. **Стереотип Spring** — помечает класс как компонент для автоматического сканирования

2. **Перевод исключений** — автоматически преобразует исключения от конкретной технологии (JPA, JDBC) в Spring-исключения

3. **Четкое разделение слоев** — указывает, что класс относится к слою доступа к данным

**Практический пример:**
``` java
// Spring автоматически создаст бин этого репозитория
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Spring Data JPA создаст реализацию во время выполнения
}

// Кастомная реализация репозитория (редкий случай)
@Repository
public class CustomUserRepositoryImpl implements CustomUserRepository {
    
    @PersistenceContext
    private EntityManager entityManager; // JPA интерфейс для работы с БД
    
    @Override
    public List<User> findComplexUsers() {
        // Сложная логика с использованием EntityManager
        return entityManager.createQuery("...", User.class).getResultList();
    }
}
```


## 5. ACID и зачем транзакции в DAO
**Теоретический материал:**
**ACID** — это набор свойств, которые гарантируют надежность транзакций:

- **А (Atomicity) - Атомарность**: Транзакция выполняется как единое целое. Либо все операции выполняются, либо ни одной. Пример: перевод денег между счетами.

- **C (Consistency) - Согласованность**: Транзакция переводит базу из одного согласованного состояния в другое. Не нарушаются ограничения целостности.

- **I (Isolation) - Изоляция**: Параллельные транзакции не мешают друг другу. Уровни изоляции определяют, насколько транзакции "видят" изменения друг друга.

- **D (Durability) - Долговечность**: После завершения транзакции изменения сохраняются даже при сбое системы.

### **Почему транзакции нужны в DAO?**
**Без транзакций:**
``` java
// ПРОБЛЕМА: если между этими операциями произойдет сбой,
// данные останутся в несогласованном состоянии
public void transferMoney() {
    accountDAO.withdraw(fromAccount, amount);  // 1. Сняли
    // Здесь может произойти сбой!
    accountDAO.deposit(toAccount, amount);     // 2. Не зачислили 😱
}
```
**С транзакциями:**
``` java
@Transactional
public void transferMoney() {
    accountDAO.withdraw(fromAccount, amount);  // 1. Сняли
    accountDAO.deposit(toAccount, amount);     // 2. Зачислили
    // Spring автоматически закоммитит транзакцию
    // ИЛИ откатит все изменения при исключении
}
```
**Уровни изоляции в Spring:**
``` java
@Transactional(isolation = Isolation.READ_COMMITTED)
public void sensitiveOperation() {
    // Читаются только закоммиченные данные
}

// Уровни изоляции (от слабого к сильному):
// READ_UNCOMMITTED - можно читать незакоммиченные данные других транзакций
// READ_COMMITTED   - читаются только закоммиченные данные (по умолчанию в большинстве БД)
// REPEATABLE_READ  - гарантирует, что повторное чтение даст те же данные
// SERIALIZABLE     - полная изоляция, транзакции выполняются последовательно
```
## 6. ORM vs JDBC (плюсы/минусы)
**Сравнительная таблица:**


Критерий|ORM (Hibernate/JPA)|Чистый JDBC
---|---|---
Количество кода|✅ Мало шаблонного кода|❌ Много повторяющегося кода
Производительность|❌ Может быть медленнее из-за накладных расходов|✅ Максимальная производительность
Безопасность от SQL-инъекций|✅ Автоматическая параметризация|❌ Нужно самостоятельно использовать PreparedStatement
Портативность между БД|✅ Легко сменить БД (диалекты Hibernate)|❌ SQL может быть специфичным для БД
Сложность запросов|❌ Сложные запросы могут быть трудными для выражения|✅ Полный контроль над SQL
Обучение|❌ Высокий порог входа|✅ Проще понять основы
Маппинг объектов|✅ Автоматический маппинг ResultSet → Object|❌ Ручной маппинг
Кэширование|✅ Встроенное кэширование 1-го и 2-го уровня|❌ Нет кэширования

### **Практический пример сравнения:**
**JDBC подход:**
``` java
@Repository
public class UserJdbcDao {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    public User findById(Long id) {
        String sql = "SELECT id, name, email FROM users WHERE id = ?";
        
        // Много ручного кода
        return jdbcTemplate.queryForObject(sql, new RowMapper<User>() {
            @Override
            public User mapRow(ResultSet rs, int rowNum) throws SQLException {
                User user = new User();
                user.setId(rs.getLong("id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                return user;
            }
        }, id);
    }
    
    public void save(User user) {
        String sql = "INSERT INTO users (name, email) VALUES (?, ?)";
        jdbcTemplate.update(sql, user.getName(), user.getEmail());
    }
}
```
**ORM подход:**
``` java
// Та же функциональность в 1 строку!
public interface UserRepository extends JpaRepository<User, Long> {
    // Spring Data JPA делает всю работу за нас
}
```

**Когда что использовать?**
- Используйте ORM (JPA/Hibernate):

    - Для большинства бизнес-приложений

    - Когда нужно быстро разрабатывать

    - Для стандартных CRUD-операций

    - Когда важна переносимость между БД

- Используйте JDBC:

    - Для сложных аналитических запросов

    - Когда нужна максимальная производительность

    - Для массовых операций (batch processing)

    - Когда работаете со сложными SQL-конструкциями

**Гибридный подход:**
Часто используют оба подхода в одном приложении: JPA для стандартных операций и JDBC для сложных запросов.

``` java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Стандартные методы через JPA
}

@Repository
public class UserCustomRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    // Сложные запросы через JDBC
    public List<UserStats> getUserStatistics() {
        String sql = "SELECT ..."; // Сложный SQL с агрегациями
        return jdbcTemplate.query(sql, new UserStatsRowMapper());
    }
}
```