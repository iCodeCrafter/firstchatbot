# firstchatbot

A Spring Boot web application with a chat endpoint powered by Spring AI's OpenAI integration.

## Run

Requires Java 21 and Maven. Set the OpenRouter API key and WeatherAPI key in your environment
or in a local `.env` file, then start the application from the project root:

```bash
export OPENROUTER_API_KEY=your-openrouter-api-key
export WEATHER_API_KEY=your-weatherapi-key
mvn spring-boot:run
```

For local development, create `.env` in the project root (it is git-ignored) with:

```properties
OPENROUTER_API_KEY=your-openrouter-api-key
WEATHER_API_KEY=your-weatherapi-key
```

The weather tool is available to the chat assistant for current conditions in a requested location.
Keep both keys out of source control; Spring loads the optional `.env` file automatically.

Spring Boot DevTools is enabled for development. Keep this command running while you work; after saving a Java change, VS Code compiles it and DevTools restarts the application automatically. The server may be briefly unavailable during the restart. You do not need to stop and start the command for each code change.

To send a chat message:

```bash
curl -X POST http://localhost:8080/api/chat \
    -H 'Content-Type: text/plain' \
    --data-binary 'Hello'
```

To summarize a support ticket, POST plain text to `/api/summarize` instead.

## Website builder

The website builder accepts plain-text requirements at `/app/api/builder/website`.
Each request is handled independently; include all requirements for the site in the
request. Generated files are stored under `generated-sites/` in the project directory.

```bash
curl -X POST http://localhost:8080/app/api/builder/website \
    -H 'Content-Type: text/plain' \
    --data-binary 'Create a responsive landing page for a neighborhood bakery'
```