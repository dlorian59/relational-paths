# relational-paths 0.3.0 — полный справочник API

[← Руководство](ru.md) · [Версии](../index.md) · [README](../../README.md) · [English](reference.en.md)

Ниже перечислены все публичные типы, группы accessors и операции. [Полный запускаемый пример](examples/Example.java) задаёт `schema`, `planner`, планы и колонки; [отчёт Spring](spring.ru.md) добавляет внедрение через конструктор и настройки. Выражения в таблице — использование внутри метода, а не самостоятельные программы.

## Публичный API

| API и исходник | Пример | Контракт и границы |
| --- | --- | --- |
| [Table(id, name, columns)](../../src/main/java/io/github/alex59/relationalpaths/Table.java) | `new Table("orders", "sales.orders", Set.of("id"))` | ID роли и колонки используют ASCII-идентификаторы; физическое имя — table или schema.table. Непустой список разрешённых колонок копируется в отсортированное неизменяемое множество. Несколько ролей могут ссылаться на одну таблицу. |
| [ColumnPair(fromColumn, toColumn)](../../src/main/java/io/github/alex59/relationalpaths/ColumnPair.java) | `new ColumnPair("customer_id", "id")` | Одна пара равенства. Направление связи определяет таблицы колонок; существование проверяет Schema. |
| [Relationship(id, from, to, cost, columns)](../../src/main/java/io/github/alex59/relationalpaths/Relationship.java) | `new Relationship("order_customer", "orders", "customer", 1, pairs)` | Положительная int-стоимость; непустой копируемый список без дубликатов пар. Одинаковые логические from/to запрещены. Обратное направление объявляется явно. |
| [Schema(tables, relationships)](../../src/main/java/io/github/alex59/relationalpaths/Schema.java) | `var schema = new Schema(tables, relationships)` | Минимум одна таблица; уникальные ID; все конечные роли и колонки связи должны существовать. Пустой набор связей допустим. Коллекции копируются. |
| [Schema.tables/relationships/table/relationship](../../src/main/java/io/github/alex59/relationalpaths/Schema.java) | `schema.table("orders")`; `schema.relationship("order_customer")` | Неизменяемые maps упорядочены лексикографически по ID. Неизвестный ID даёт IllegalArgumentException, null — NullPointerException. Публичного метода исходящих рёбер нет. |
| [SearchLimits and DEFAULT](../../src/main/java/io/github/alex59/relationalpaths/SearchLimits.java) | `new SearchLimits(32, 64, 64)` | Лимиты метаданных и одного автоматического поиска, а не строк, времени SQL или числа запрошенных целей. Значения ниже. |
| [JoinPlanner(schema) / (schema, limits)](../../src/main/java/io/github/alex59/relationalpaths/JoinPlanner.java) | `var planner = new JoinPlanner(schema, limits)` | Первый конструктор использует SearchLimits.DEFAULT; оба сразу отклоняют слишком большую схему. Метаданные планировщика неизменяемы. |
| [JoinPlanner.plan(root, targets)](../../src/main/java/io/github/alex59/relationalpaths/JoinPlanner.java) | `planner.plan("orders", List.of("customer", "region"))` | Минимальная сумма положительных смысловых стоимостей к каждой цели; равная стоимость неоднозначна даже при разном числе переходов. Дубликаты целей объединяются; пустой список даёт план только корня. |
| [JoinPlanner.plan(root, targets, explicitRoutes)](../../src/main/java/io/github/alex59/relationalpaths/JoinPlanner.java) | `planner.plan("orders", List.of("address"), Map.of("address", List.of("shipping_ref")))` | Переопределяет только запрошенные цели. Маршрут начинается в корне, идёт по связным рёбрам, заканчивается в цели и не повторяет роль. Может стоить больше кратчайшего. |
| [JoinPlan.schema/root/joins](../../src/main/java/io/github/alex59/relationalpaths/JoinPlan.java) | `plan.schema()`; `plan.root()`; `plan.joins()` | Проверенный план создаёт только планировщик. JOIN упорядочены по зависимостям; каждая конечная роль встречается один раз. Схема и коллекции результата неизменяемы. |
| [JoinPlan.aliases/routes/explain](../../src/main/java/io/github/alex59/relationalpaths/JoinPlan.java) | `plan.aliases()`; `plan.routes()`; `plan.explain()` | Алиасы начинаются с t0 для корня и идут в порядке JOIN. Routes отсортированы по запрошенной цели и содержат ID связей; промежуточные роли не обязаны быть ключами. Explain — диагностический текст, не SQL и не машинный формат обмена. |
| [ColumnRef(table, column)](../../src/main/java/io/github/alex59/relationalpaths/ColumnRef.java) | `new ColumnRef("customer", "name")` | Использует ID логической роли, а не физическое имя или алиас. Синтаксис проверяется в конструкторе, существование и включение в план — при рендеринге. |
| [Filter(column, operator, value) and Operator](../../src/main/java/io/github/alex59/relationalpaths/Filter.java) | `new Filter(new ColumnRef("customer", "name"), Filter.Operator.EQ, "Alice")` | Колонка и оператор non-null; значение null допустимо только для EQ и NE. Тип и ссылка значения сохраняются. Все фильтры соединяются AND; шесть операторов перечислены ниже. |
| [SqlRenderer.select(plan, projection, filters)](../../src/main/java/io/github/alex59/relationalpaths/SqlRenderer.java) | `SqlRenderer.select(plan, projection, filters)` | Непустая упорядоченная проекция; фильтры могут быть пустыми, порядок сохраняется. Каждая колонка должна быть объявлена и включена в план. Рендерит полный INNER JOIN план, двойные кавычки, c0/c1/… и JDBC-параметры ?. |
| [BoundSql(sql, bindings), sql(), bindings()](../../src/main/java/io/github/alex59/relationalpaths/BoundSql.java) | `query.sql()`; `query.bindings()` | Копирует список, но не объекты параметров. SQL, список и элементы non-null. Ручное создание принимает произвольный SQL и не проверяет число параметров или идентификаторы; гарантии SqlRenderer не распространяются на написанный вручную SQL. |
| [PlanningException(message)](../../src/main/java/io/github/alex59/relationalpaths/PlanningException.java) | `catch (PlanningException failure)` | Наследует IllegalArgumentException. Требуется изменение метаданных, целей, явного выбора или лимитов; повтор тех же данных не устраняет неоднозначность. |
| [AmbiguousPathException.target/candidates](../../src/main/java/io/github/alex59/relationalpaths/AmbiguousPathException.java) | `failure.target()`; `failure.candidates()` | Наследует PlanningException. Два детерминированных маршрута-свидетеля из ID, а не полный список; вложенные списки неизменяемы. Для выбора пути перехватывайте раньше общего исключения. |

Все публичные records имеют одноимённые компоненты и стандартные Java `equals/hashCode/toString`. Например, `Relationship.id/from/to/cost/columns`, `Table.id/name/columns`, `ColumnPair.fromColumn/toColumn`, `ColumnRef.table/column`, `Filter.column/operator/value` и `SearchLimits.maxTables/maxRelationships/maxEdgeVisits` — accessors только для чтения. Они не планируют маршрут и не выполняют SQL.

## Настройка поиска

| Настройка | По умолчанию | Граница и момент проверки |
| --- | --- | --- |
| maxTables | 512 | Целое ≥ 1; учитываются все роли схемы, включая недостижимые. Проверяется в конструкторе JoinPlanner. |
| maxRelationships | 8192 | Целое ≥ 0; учитываются все рёбра схемы. Проверяется при создании планировщика. |
| maxEdgeVisits | 8192 | Целое ≥ 0; просмотренные исходящие рёбра одного автоматического поиска. Бюджет могут расходовать посторонние достижимые ветви. |

План только корня или запрос с явными маршрутами ко всем некорневым целям не запускает автоматический поиск и может работать с maxEdgeVisits=0. Лимиты размера схемы сохраняются. Явные маршруты всегда проверяются. В core нет файла конфигурации; [стартер](spring.ru.md) переводит properties в тот же SearchLimits.

## Операторы, проекции и параметры

| Оператор | Значение non-null | Значение null |
| --- | --- | --- |
| EQ | `= ?` | `IS NULL`, без параметра |
| NE | `<> ?` | `IS NOT NULL`, без параметра |
| LT | `< ?` | ошибка |
| LE | `<= ?` | ошибка |
| GT | `> ?` | ошибка |
| GE | `>= ?` | ошибка |

Фильтры соединяются AND в порядке входа; bindings содержит только non-null значения в порядке параметров. Индексы JDBC начинаются с единицы. Пустые фильтры исключают WHERE; пустая проекция недопустима. Повторяющиеся колонки проекции допустимы и получают разные cN-алиасы. Нет приведения типов, разбора LIKE/IN/OR и привязки драйвером БД. Поток показан в [примерах rendering/null](examples/Example.java) и [SQL-проверках](../../src/test/java/io/github/alex59/relationalpaths/SqlRendererTest.java).

## Граничные случаи и жизненный цикл

| Случай | Результат / ответственность |
| --- | --- |
| Пустые цели | Корень t0, без JOIN и без записей routes. Явно запросите корень, чтобы получить его пустой маршрут. |
| Дубликаты целей / перестановка метаданных | Тот же детерминированный план; порядок проекции/фильтров намеренно сохраняется. |
| Общие пути | Совместимые участки переиспользуются. Это не оптимизатор глобально минимального дерева JOIN/Steiner. |
| Циклы положительной стоимости | Автоматический поиск завершается без перебора всех маршрутов. Явный путь не может повторять роль. |
| Больше двух равных путей | Двух свидетелей достаточно для отказа по неоднозначности; candidates() не каталог маршрутов. |
| Владение | Коллекции схемы/плана — снимки. Объекты параметров остаются ссылками вызывающего кода; используйте стабильные JDBC-значения. Нет фоновых задач, кэша, соединения или close hook. |
| Конкурентность | Неизменяемые схема, планировщик и план допускают совместное использование. Не меняйте одновременно аргументы вызова и объекты параметров. При смене метаданных пересоздайте планировщик. |
| Границы безопасности | Разрешённые имена/колонки не авторизуют пользователя. Схема должна быть доверенной, права, tenant-фильтры и разрешения БД проверяет приложение. |
| Семантика SQL | INNER JOIN может исключать строки и размножать один-ко-многим. Нет автоматической оценки кардинальности, дедупликации, outer join, группировки, пагинации и управления транзакциями. |

Для своего SQL-диалекта используйте неизменяемый JoinPlan напрямую. Встроенный рендерер использует двойные кавычки ANSI и JDBC ?, но не выполняет запрос в живой БД. Компромиссы описаны в [архитектуре и ограничениях](../architecture.md).

## Ошибки и диагностика

| Ошибка/симптом | Причина | Действие |
| --- | --- | --- |
| `Invalid identifier`, `Expected table or schema.table` | Пунктуация, пробелы, цифра в начале, кавычки/raw SQL или лишние части имени. | Используйте объявленные ASCII-имена `[A-Za-z_][A-Za-z0-9_]*`; кавычки добавляет рендерер. |
| `Duplicate table/relationship`, `Unknown endpoint/column` | Метаданные противоречивы ещё до планирования. | Исправьте ID и включите обе колонки каждого равенства в разрешённые наборы таблиц. |
| `Self-joins need distinct logical table roles` | Связь ведёт из роли в неё же. | Задайте каждой позиции отдельный ID роли при одном физическом имени. |
| `Unknown table/relationship` | Неизвестный корень/цель или ID явной связи. | Используйте ID из Schema, а не физические имена. |
| `No directed path from …` | Объявленный граф не достигает цели. | Проверьте направление; добавьте разрешённую обратную связь только если это верно по смыслу. |
| `Ambiguous path to …` | Минимальная сумма стоимости одинакова минимум у двух путей. | Изучите target()/candidates(), задайте явный путь ID либо разделите роли. Не выбирайте первого свидетеля автоматически. |
| `Explicit route supplied for an unrequested target`, `Disconnected`, `repeats role`, `ends at`, `too long` | Заданный путь не является простым связным маршрутом от корня к запрошенной цели. | Исправьте весь путь; максимум переходов — число ролей схемы минус один. |
| `Conflicting paths enter role …` | Запрошенные пути спорят о входящей связи общей роли. | Выберите согласованные явные пути либо отдельные роли; планировщик не переписывает бизнес-выбор молча. |
| `Invalid search bounds`, `Schema exceeds configured table/relationship limits`, `Search exceeded maxEdgeVisits=…` | Неверные лимиты, слишком большая схема или исчерпан автоматический обход. | Проверьте настройки; сократите разрешённую схему либо осознанно увеличьте нужный лимит. Частичный план не возвращается. |
| `At least one projection is required`, `Undeclared column`, `Table is absent from plan` | Пустая проекция либо колонка вне разрешённого списка/дерева JOIN. | Объявите колонку и включите логическую роль в цели; явно задавайте проекции. |
| `Null supports only EQ and NE` | Упорядоченное сравнение получило null. | Для проверки null используйте EQ/NE, иначе передайте типизированное JDBC-значение non-null. |
| `NullPointerException` / `UnsupportedOperationException` | Отсутствует обязательная структура / попытка изменить неизменяемый контейнер. | Исправьте null-аргументы; для редактирования сделайте свою копию. Допустимый null в Filter — описанное исключение. |

PlanningException является IllegalArgumentException; AmbiguousPathException — более конкретный тип. При переводе ошибок в ответ приложения перехватывайте неоднозначность первой. Фрагменты сообщений помогают диагностике; логика приложения должна использовать типы исключений и структурированные target()/candidates(), а не разбирать explain() или текст ошибки.

## Spring и границы AOP

[Стартер](spring.ru.md) предоставляет обычные beans; он не оборачивает планирование прокси, транзакцией или повтором. JoinPlanner — final, SqlRenderer.select — static; для независимой политики аудита/времени перехватывайте границу сервиса приложения. Class-based Spring proxy не наследует final-класс, self-invocation обходит advice: [правила Spring](https://docs.spring.io/spring-framework/reference/core/aop/proxying.html). Аспект не должен молча разрешать неоднозначность или менять решения авторизации.

## Проверки и версии

Запустите `./gradlew test example` на установленном JDK 17+; [полный обычный Java-пример](examples/Example.java) имеет [команды компиляции/запуска](ru.md#запуск-полного-примера-и-решение-проблем). Существующие [тесты планировщика](../../src/test/java/io/github/alex59/relationalpaths/JoinPlannerTest.java) включают сравнение с полным перебором небольших графов и проверки детерминизма. [Матрица Java/версий](java.ru.md) отделяет базу артефакта от runtime; исходники и байткод остаются на 17.

[← Руководство](ru.md)
