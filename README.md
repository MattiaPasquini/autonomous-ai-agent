# Autonomous AI Agent / Agente AI Autonomo

This is a thesis project for Bachelor of Science in Computer Engineering, SUPSI DTI.

# Introduction
This thesis project focus on the development of a Spring Boot-based Java application (self-host) designed to act as an intelligent agent for managing the local file system.
The goal is to create a system capable of interpreting complex commands expressed in natural language, such as "Find all .log files, summarize the errors, and move the files to an archive folder" and executing them autonomously.
Furthermore, the agent must operate within a restricted workspace, without the ability to access files or directories outside of it.

## Disclaimer
As said in the introduction, the priority is focusing on developing on AI agent, more than handling user auth, persistence db, etc. But at the end the project is extensible, improving and adding the features (see the section [Potential future features](#potential-future-features)).

# Project structure
Originally this repository was on the SUPSI's GitLab (that's why there's `.gitlab-ci.yml`).

- `src`: contains the source code of the project, containing the implementation and testing
- `doc`: documentation of the thesis project. Since SUPSI DTI is located in Italian speaking part of Switzerland, the documentation is written in Italian, except the abstract (page `xi`).

# Tech stack
- Backend: Java 21, Spring Boot 4.0.6
- Frontend: HTML, CSS and Javascript (I'm not frontend engineer tho)
- AI SDK: Google GenAI Java SDK
- Version Control & CI/CD: GitLab and GitLab CI/CD
- Testing: JUnit 5 and Mockito

# Architecture overview

```
Browser (HTML/CSS/JS)
        |  REST
        V
Controllers --> Services --> AgentClient (interface)
 (AgentAI,                        │
  Sandbox)                        V
                          AgentClientGemini --> Google Gemini API
                                  │
                                  V
                            ToolRegister
                                  │  reflection
                                  V
                 Filesystem tools (Find, Read, Write, Create,
                 Copy, Remove) --> PathValidator --> Sandbox dir
```
(Thanks LLM for this amazing diagram)
## Main components

- **Controllers** (`controller`): REST endpoints for chat, confirmation and sandbox selection.
- **Agent client** (`ai`): `AgentClient` is an interface that hides which LLM is used. `AgentClientGemini` implements it and runs the loop: call the model, execute the requested function calls, send the results back, repeat until a plain-text answer.
- **Tools** (`tool`): filesystem operations (find, read, write, create, copy, remove) as Java methods annotated with `@AiTool`.
- **Tool registry** (`ToolRegister`): discovers the annotated methods (`@AiTools`) by reflection and exposes them to the model as function declarations, so adding a tool doesn't touch the loop.

## Safety mechanisms

- **Sandbox**: `PathValidator` normalizes every path and rejects anything outside the sandbox root.
- **Human in the loop**: tools annotated with `@HumanInTheLoop` pause the loop and ask user if the tool can be executed or not. Based on the user's answer, a tool is executed or reports the denial to the model.
- **Error feedback**: tool exceptions are returned to the model as results, not thrown.

## Request flow

1. The user selects a sandbox directory.
2. The user sends a natural language command through the chat.
3. The agent asks the LLM what to do, and the LLM answers with one or more function calls.
4. Safe tools run immediately, and their results go back to the LLM. For a destructive tool the loop stops and asks the user for confirmation.
5. Steps 3-4 repeat until the LLM produces a final answer, which is shown in the chat.

# Potential future features
This project is extensible adding new features such as:
- Adding more tools (creating symlink, showing tree structure of a directory, shell command execution, etc)
- Creating a persistent system that allows conversation between the user and the AI agent to be saved so they can be reused at another moment.
- Multi-provider LLM support
- real-time stream of AI thoughts and responses.

---

# How to run it

## Prerequisites

- Java 21+
- Maven 3.8+
- A valid [Google Gemini API key](https://aistudio.google.com/app/apikey)

## Environment Variable Setup

The application requires the `GOOGLE_GENAI_API_KEY` environment variable to be set before running.

Rename the file `.env.example` to `.env` and save the API key in the `GOOGLE_GENAI_API_KEY` environment variable.

The `.env` file is read from the working directory, i.e. the folder you launch `java -jar` from (not necessarily the folder containing the JAR). If it is missing, the application starts but calls to Gemini will fail. Alternatively, set `GOOGLE_GENAI_API_KEY` as a system environment variable.

## Choosing Gemini's model

In the `application.properties` file located in `src/main/resources` you can edit the property `google.genai.model`.
Currently the property is set as `gemini-3.5-flash-lite`.

## Build

Clone the repository and build the project using the Maven wrapper:

```bash
./mvnw clean package
```

On Windows:
```powershell
.\mvnw.cmd clean package
```

(Or use mvn in your environment)

This produces a runnable JAR in the `target/` directory.

## Run

```bash
java -jar target/agente-ai-autonomo-0.0.5.jar
```

The server starts on `http://127.0.0.1:8080`.

---
# Author
Mattia Pasquini

Supervisor: Matteo Besenzoni

June-August 2026
