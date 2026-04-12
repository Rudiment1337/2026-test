Введение
Spring Framework — это не просто «фреймворк для веба». Его сердце — Spring Core, модуль, отвечающий за инверсию управления (IoC) и внедрение зависимостей (DI). Именно он позволяет писать гибкий, тестируемый и масштабируемый код.

Сегодня мы разберём:

Что такое IoC и DI — и зачем они нужны.
Как Spring управляет объектами (бинами).
Как работает контейнер.
Что такое скоупы и как они влияют на многопоточность.
Реальный пример без Spring Boot — только Spring Core.
🧩 1. Инверсия управления (IoC) и Внедрение зависимостей (DI)
❓ Что такое IoC?
Inversion of Control (IoC) — принцип, при котором управление созданием и связыванием объектов передаётся контейнеру, а не самому коду.

📌 Без IoC:

```
public class OrderService {
    private PaymentService paymentService = new PaymentService(); // Жёсткая связь!
}
```
📌 С IoC:

```
public class OrderService {
    private PaymentService paymentService;

    public OrderService(PaymentService paymentService) { // Зависимость "внедряется"
        this.paymentService = paymentService;
    }
}
```
→ Кто-то снаружи (контейнер) решает, какой экземпляр PaymentService передать.

🧵 Что такое DI?
Dependency Injection (DI) — это способ реализации IoC: зависимости передаются объекту извне, а не создаются внутри.

Способы внедрения:

Через конструктор (рекомендуется)
Через сеттер
Через поля (с @Autowired — удобно, но менее гибко)
🏭 2. Контейнер Spring (IoC Container)
Что это?
Контейнер — это «фабрика объектов», которая:

Создаёт бины (объекты, управляемые Spring)
Связывает их зависимости
Управляет их жизненным циклом
Типы контейнеров:
BeanFactory — базовый, ленивая инициализация
ApplicationContext — расширенный, поддерживает аннотации, события, (i18n) и т.д.
📌 Мы будем использовать AnnotationConfigApplicationContext.

🔄 3. Жизненный цикл Bean
Bean — это любой объект, управляемый Spring-контейнером.

Этапы жизненного цикла:
Создание экземпляра (через конструктор или фабричный метод)
Внедрение зависимостей (@Autowired, @Value, и т.д.)
Вызов @PostConstruct — метод, помеченный этой аннотацией, вызывается после инициализации
Bean готов к использованию
Вызов @PreDestroy — при закрытии контейнера (если singleton и контейнер корректно закрыт)
Уничтожение
📌 Пример:
```
@Component
public class LoggerService {

    public LoggerService() {
        System.out.println("1. [LoggerService] Создан");
    }

    @PostConstruct
    public void init() {
        System.out.println("3. [LoggerService] Инициализирован (@PostConstruct)");
    }

    @PreDestroy
    public void destroy() {
        System.out.println("5. [LoggerService] Уничтожен (@PreDestroy)");
    }
}
```
🧪 4. Почему IoC упрощает тестирование?
Потому что зависимости внедряются извне, а не создаются внутри.

📌 Пример:

```
@Service
public class OrderService {
    private final PaymentService paymentService;

    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public void processOrder() {
        paymentService.charge(); // Мы можем подменить paymentService на мок!
    }
}
```
→ В тесте:

```
@Test
void testProcessOrder() {
    PaymentService mockPayment = mock(PaymentService.class);
    OrderService service = new OrderService(mockPayment); // внедряем мок!
    service.processOrder();
    verify(mockPayment).charge();
}
```
✅ Без IoC — вы бы создавали new PaymentService() внутри — и не смогли бы подменить его на мок.

🌐 5. Скоупы (@Scope) — связь с многопоточностью
Что такое Scope?
Scope определяет, сколько экземпляров бина существует и как долго они живут.

Основные скоупы:
SCOPE|Описание|Потокобезопасность
---|---|---
singleton (по умолчанию)|Один экземпляр на контейнер. Создаётся при старте.|❌ Нет (если есть состояние)
prototype|Новый экземпляр при каждом запросе.|✅ Да (если не используется повторно)
request|Один экземпляр на HTTP-запрос (только в веб-приложениях)|✅ Да (на время запроса)
session|Один экземпляр на HTTP-сессию|❌ Нет (если сессия общая)

⚠️ Важно: Singleton и многопоточность
Если у singleton-бина есть изменяемое состояние — он не потокобезопасен!

📌 Пример проблемы:

```
@Component
@Scope("singleton")
public class CounterService {
    private int count = 0; // ❌ Опасное состояние!

    public void increment() {
        count++; // Гонка потоков!
    }

    public int getCount() {
        return count;
    }
}
```
→ В многопоточной среде значение count будет непредсказуемым.

✅ Решение:

Делать бины stateless (без внутреннего состояния)
Использовать prototype, если состояние необходимо
Использовать синхронизацию (не рекомендуется — снижает производительность)
💻 6. Практический пример: Внедрение зависимостей без Spring Boot
Создадим простое приложение с использованием только Spring Core.

📁 Структура проекта (Maven):


```
src/
 └── main/
      └── java/
           └── com.example/
                ├── config/AppConfig.java
                ├── service/MessageService.java
                ├── service/EmailService.java
                └── Main.java
```
1️⃣ Интерфейс и реализация
MessageService.java

```
public interface MessageService {
    String getMessage();
}
```
EmailService.java

```
import org.springframework.stereotype.Service;

@Service
public class EmailService implements MessageService {
    @Override
    public String getMessage() {
        return "Hello from EmailService!";
    }
}
```
2️⃣ Сервис, который использует MessageService
NotificationService.java

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class NotificationService {

    @Autowired  // Внедрение зависимости через поле
    private MessageService messageService;

    @PostConstruct
    public void init() {
        System.out.println("NotificationService инициализирован. Сообщение: " + messageService.getMessage());
    }

    public void sendNotification() {
        System.out.println("Отправка: " + messageService.getMessage());
    }
}
```
3️⃣ Конфигурация контекста
AppConfig.java

```java
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.example") // Указываем, где искать @Component, @Service и т.д.
public class AppConfig {
    // Пустой класс — конфиг через аннотации
}
```

4️⃣ Запуск приложения
Main.java

```java
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import com.example.service.NotificationService;

public class Main {
    public static void main(String[] args) {
        // Создаём контейнер вручную
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(AppConfig.class);

        // Получаем бин из контейнера
        NotificationService notificationService = context.getBean(NotificationService.class);

        // Используем
        notificationService.sendNotification();

        // Закрываем контейнер (вызовет @PreDestroy, если есть)
        context.close();
    }
}
```

✅ Что происходит при запуске:
Spring сканирует пакет com.example.
Находит @Service: EmailService, NotificationService.
Создаёт singleton-бин EmailService.
Создаёт singleton-бин NotificationService.
Внедряет EmailService в NotificationService через @Autowired.
Вызывает @PostConstruct → выводит сообщение.
Мы вызываем sendNotification() → использует внедрённый сервис.
context.close() → если бы были @PreDestroy — вызвал бы их.
📊 Визуализация работы контейнера

```
[Контейнер]
   │
   ├── Создаёт EmailService (singleton)
   │
   └── Создаёт NotificationService
         │
         └── Внедряет EmailService (@Autowired)
               │
               └── Вызывает @PostConstruct
                     │
                     └── Готов к использованию
```
🧠 Почему это мощно?
✅ Гибкость: можно легко заменить EmailService на SMSService, реализующий тот же интерфейс — без изменения NotificationService.
✅ Тестируемость: внедряем моки.
✅ Управление жизнью: контейнер сам создаёт, инициализирует, уничтожает.
✅ Конфигурируемость: можно управлять scope, условиями создания, профилями и т.д.
📚 Выводы
✅ IoC — передача управления созданием объектов контейнеру.
✅ DI — механизм внедрения зависимостей (через конструктор, поля, сеттеры).
✅ Контейнер (ApplicationContext) — фабрика, управляющая бинами.
✅ Жизненный цикл — от создания до уничтожения, с хуками @PostConstruct и @PreDestroy.
✅ Скоупы — определяют, сколько экземпляров и как долго живут → критично для многопоточности.
✅ Singleton ≠ потокобезопасность — делайте бины stateless!

📖 Домашнее задание
Добавьте в проект SMSService, реализующий MessageService.
→ Сделайте так, чтобы NotificationService использовал его вместо EmailService (через @Primary или @Qualifier — по желанию).
Создайте бин с @Scope("prototype") и проверьте, что при каждом getBean() создаётся новый экземпляр.
Добавьте в один из сервисов изменяемое состояние (например, счётчик) — проверьте, как он ведёт себя при многократном вызове в singleton-режиме.
Дополнительно: Реализуйте внедрение зависимости через конструктор (без @Autowired на поле) — это best practice!
💬 Вопросы для обсуждения
Почему внедрение через конструктор считается лучшей практикой?
Можно ли использовать @Autowired на статических полях? Почему?
Что произойдёт, если два бина реализуют один интерфейс, и вы попытаетесь внедрить его без @Qualifier?
Почему prototype-бин не вызывает @PreDestroy при закрытии контейнера?