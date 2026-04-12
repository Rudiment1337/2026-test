# Лекция 5: Spring MVC и параметры запросов

## Цель лекции
Научиться работать с параметрами HTTP-запросов в Spring MVC, понимать
разницу между `@RequestParam` и `@PathVariable`, а также разобраться в
особенностях GET-запросов, безопасности и управлении потоком данных в
HTTP.

---

## 1. Spring MVC: основы

Spring MVC — это модуль Spring Framework для построения веб-приложений
на основе паттерна **Model-View-Controller**.

В современных приложениях (особенно RESTful API) часто используется
только **контроллер и модель данных**, без шаблонов (JSP/Thymeleaf),
но базовые принципы остаются.

### Основные аннотации:
- `@RestController` — контроллер, возвращающий данные (JSON, текст).
- `@RequestMapping` / `@GetMapping`, `@PostMapping` — маршрутизация.
- `@RequestParam`, `@PathVariable` — извлечение данных из запроса.

---

## 2. Параметры запроса: @RequestParam

Используется для получения **GET-параметров** из URL.

Пример URL:

```/search?query=java&limit=10```

### Пример в Spring:
```java
@RestController
public class SearchController {

@GetMapping("/search")
public String search(@RequestParam String query) {
    return "Результаты поиска по запросу: " + query;
    }
}
```
➡️ При переходе на `/search?query=java` — получим:
Результаты поиска по запросу: java

________________________________

Дополнительные возможности `@RequestParam`

1. Необязательные параметры

```java

@GetMapping("/search")
public String search(
@RequestParam(required = false) String query
) {
    if (query == null) {
        return "Введите запрос";
    }
    return "Поиск: " + query;
}
```
1. Значение по умолчанию

```java
@GetMapping("/search")
public String search(
@RequestParam(defaultValue = "все") String category
) {
    return "Категория: " + category;
}
```
3. Множественные значения (массивы)

```java
@GetMapping("/filter")
public String filter(@RequestParam List<String> tags) {
    return "Фильтр по тегам: " + tags;
}
```
Пример: `/filter?tags=java&tags=spring&tags=web`

________________________________

3. Путь как параметр: `@PathVariable`

Используется, когда параметр — часть URL-пути.

Пример URL:

`/users/123`

Пример:

```java
@GetMapping("/users/{id}")
public String getUser(@PathVariable Long id) {
    return "Пользователь с ID: " + id;
}
```
Можно указывать имя явно:

```java
@GetMapping("/books/{bookId}")
public String getBook(@PathVariable("bookId") Long id) {
    return "Книга №" + id;
}
```
✅ Полезно для REST API: `/api/products/{id}, /posts/{slug}` и т.д.

________________________________

1. Модель данных: передача в шаблон (опционально)

Если вы используете шаблоны (например, Thymeleaf), можно передавать
данные через `Model`.

```java
@Controller
public class FormController {

@GetMapping("/form")
public String showForm(Model model) {
model.addAttribute("message", "Добро пожаловать!");
    return "search-form"; // имя шаблона (search-form.html)
}
}
```
Шаблон (templates/search-form.html):

```html
<!DOCTYPE html>
    <html>
    <body>
        <h1 th:text="${message}"></h1>
        <form action="/search" method="get">
        <input type="text" name="query" placeholder="Поиск..." />
        <button type="submit">Найти</button>
        </form>
    </body>
</html>
```
________________________________

1. Теория: GET-параметры и их особенности

Как кодируются GET-параметры?

Передаются в строке запроса после ?.
Пары ключ=значение, разделённые &.
Специальные символы кодируются в URL encoding (percent-encoding).

Пример:
`/search?query=привет+мир&lang=ru`

`→ пробел → + или %20, русский → %D0%BF%D1%80...`

Spring автоматически декодирует параметры — вам не нужно об этом заботиться.

________________________________

Почему пароли нельзя передавать в GET?

Остается в истории браузера
Логируется веб-сервером (в access.log)
Передаётся в Referer-заголовке при переходе на другие сайты
Виден в адресной строке

✅ Правило:

Всё чувствительное (пароли, токены, персональные данные) — только
через POST, PUT, PATCH и в теле запроса.

________________________________


1. Пример: Поисковая форма

Создадим простую форму поиска с обработкой ?query=....

Шаг 1: Контроллер

```java
@Controller
public class SearchController {

    @GetMapping("/")
    public String showForm() {
        return "search";
    }

    @GetMapping("/search")
    public String search(
    @RequestParam String query,
        Model model
    ) {
        model.addAttribute("query", query);
        model.addAttribute("results", List.of(
            "Результат 1 по запросу '" + query + "'",
            "Результат 2"
            ));
        return "results";
    }
}
```
Шаг 2: Форма (templates/search.html)

```html
<!DOCTYPE html>
<html>
    <body>
        <h2>Поиск</h2>
        <form action="/search" method="get">
            <input type="text" name="query" placeholder="Введите запрос" required />
            <button type="submit">Найти</button>
        </form>
    </body>
</html>
```
Шаг 3: Результаты (templates/results.html)

```html
<!DOCTYPE html>
<html>
    <body>
        <h2>Результаты по запросу: <span th:text="${query}"></span></h2>
        <ul>
        <li th:each="result : ${results}" th:text="${result}"></li>
        </ul>
        <a href="/">← Назад</a>
    </body>
</html>
```
Как работает:

Пользователь заходит на / → видит форму.
Вводит java → форма отправляет GET-запрос: /search?query=java.
Сервер обрабатывает, передаёт данные в шаблон → показывает результаты.

________________________________

8. Выводы

Особенность | Spring
---|---
Параметры из URL|@RequestParam
Переменные в пути|@PathVariable
Обязательные/необязательные поля|required,defaultValue
Передача данных в шаблон|Model
Чувствительные данные|Только в теле POST-запроса
Управление потоком|HTTP/2, lazy loading, оптимизация ресурсов