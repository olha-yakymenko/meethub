# MeetHub

**MeetHub** is a Spring Boot web application designed to simplify the organization and management of meetings, workshops, and social events. It combines a RESTful API with a server-side Thymeleaf frontend, providing a structured environment for organizers and participants to interact, collaborate, and share feedback.


---

## 🚀 Key Features

* **Meeting Management**: Create, publish, and manage event details effortlessly.
* **Participation & Engagement**: Users can join events, leave comments, and rate their experiences.
* **Task Management**: Organizers can assign specific tasks to participants to ensure event goals are met.
* **Post-Event Analytics**: Automatically generate statistics after a meeting to analyze engagement and outcomes.
* **Role-Based Access Control**: Distinct functionalities for **Organizers** (management tools) and **Participants** (interaction tools).
* **API Documentation**: Fully documented with **Swagger UI** for easy testing and integration.

## 🛠 Tech Stack

* **Backend**: Java / Spring Boot
* **Build Tool**: Maven
* **Documentation**: Swagger / OpenAPI
* **Testing**: JUnit 5, Mockito, JaCoCo

---

## 🚦 Getting Started


### Running the Application
Before starting the application, make sure the database is running.
Docker configuration is located in the `docker/` directory.

```bash
cd docker
docker-compose up -d
```

To launch the application locally, use the following command:
```bash
mvn spring-boot:run
```

## API Documentation

Once the server is running, you can access the interactive Swagger documentation at:  
🔗 http://localhost:8080/swagger-ui/index.html

---

## 🧪 Testing & Quality Assurance

This project follows strict testing standards to ensure reliability and code quality.

- **Unit Tests** – Core logic validation  
- **Mocked Tests** – Isolating components using Mockito  
- **Parameterized Tests** – Robust validation across various input data sets  

To run the full test suite and generate a **JaCoCo coverage report**, use:

```bash
mvn clean test jacoco:report
```