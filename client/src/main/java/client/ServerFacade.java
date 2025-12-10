package client;

import com.google.gson.Gson;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ServerFacade {

    private final String baseUrl;
    private final Gson gson = new Gson();
    private int lastStatusCode = -1;

    public ServerFacade(int port) {
        this.baseUrl = "http://localhost:" + port;
    }

    public int getStatusCode() {
        return lastStatusCode;
    }

    // ===== Response classes =====

    public static class AuthResult {
        private String authToken;
        private String username;
        public String message;

        public String getAuthToken() { return authToken; }
        public String getUsername() { return username; }
        public String getMessage() { return message; }
    }

    public static class CreateGameResult {
        private Integer gameID;
        public String message;

        public Integer getGameID() { return gameID; }
        public String getMessage() { return message; }
    }

    public static class GameInfo {
        private int gameID;
        private String whiteUsername;
        private String blackUsername;
        private String gameName;

        public int getGameID() { return gameID; }
        public String getWhiteUsername() { return whiteUsername; }
        public String getBlackUsername() { return blackUsername; }
        public String getGameName() { return gameName; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GameInfo)) return false;
            GameInfo g = (GameInfo) o;
            return gameID == g.gameID &&
                    ((whiteUsername == null && g.whiteUsername == null) ||
                            (whiteUsername != null && whiteUsername.equals(g.whiteUsername))) &&
                    ((blackUsername == null && g.blackUsername == null) ||
                            (blackUsername != null && blackUsername.equals(g.blackUsername))) &&
                    ((gameName == null && g.gameName == null) ||
                            (gameName != null && gameName.equals(g.gameName)));
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(gameID)
                    ^ (whiteUsername == null ? 0 : whiteUsername.hashCode())
                    ^ (blackUsername == null ? 0 : blackUsername.hashCode());
        }
    }

    public static class ListGamesResult {
        private GameInfo[] games;
        public String message;

        public GameInfo[] getGames() { return games; }
        public String getMessage() { return message; }
    }

    public static class GenericResult {
        public String message;
        public String getMessage() { return message; }
    }

    private static class ErrorResponse {
        private String message;
        public String getMessage() { return message; }
    }

    // ===== API CALLS =====

    public AuthResult register(String username, String password, String email) {
        var reqMap = new SimpleJson();
        reqMap.put("username", username);
        reqMap.put("password", password);
        reqMap.put("email", email);

        try {
            String resp = doRequest("POST", "/user", gson.toJson(reqMap), null);

            if (lastStatusCode == 200) {
                return gson.fromJson(resp, AuthResult.class);
            }

            ErrorResponse err = gson.fromJson(resp, ErrorResponse.class);
            AuthResult r = new AuthResult();
            r.message = err == null ? resp : err.getMessage();
            return r;

        } catch (IOException e) {
            AuthResult r = new AuthResult();
            r.message = e.getMessage();  // CLEAN error
            lastStatusCode = 500;
            return r;
        }
    }

    public AuthResult login(String username, String password) {
        var reqMap = new SimpleJson();
        reqMap.put("username", username);
        reqMap.put("password", password);

        try {
            String resp = doRequest("POST", "/session", gson.toJson(reqMap), null);

            if (lastStatusCode == 200) {
                return gson.fromJson(resp, AuthResult.class);
            }

            ErrorResponse err = gson.fromJson(resp, ErrorResponse.class);
            AuthResult r = new AuthResult();
            r.message = err == null ? resp : err.getMessage();
            return r;

        } catch (IOException e) {
            AuthResult r = new AuthResult();
            r.message = e.getMessage(); // CLEAN error
            lastStatusCode = 500;
            return r;
        }
    }

    public GenericResult logout(String authToken) {
        try {
            String resp = doRequest("DELETE", "/session", null, authToken);
            GenericResult r = new GenericResult();

            if (lastStatusCode == 200) {
                r.message = null;
            } else {
                ErrorResponse err = gson.fromJson(resp, ErrorResponse.class);
                r.message = err == null ? resp : err.getMessage();
            }
            return r;

        } catch (IOException e) {
            GenericResult r = new GenericResult();
            r.message = e.getMessage(); // CLEAN error
            lastStatusCode = 500;
            return r;
        }
    }

    public CreateGameResult createGame(String gameName, String authToken) {
        var req = new SimpleJson();
        req.put("gameName", gameName);

        try {
            String resp = doRequest("POST", "/game", gson.toJson(req), authToken);

            if (lastStatusCode == 200) {
                return gson.fromJson(resp, CreateGameResult.class);
            }

            ErrorResponse err = gson.fromJson(resp, ErrorResponse.class);
            CreateGameResult r = new CreateGameResult();
            r.message = err == null ? resp : err.getMessage();
            return r;

        } catch (IOException e) {
            CreateGameResult r = new CreateGameResult();
            r.message = e.getMessage(); // CLEAN error
            lastStatusCode = 500;
            return r;
        }
    }

    public ListGamesResult listGames(String authToken) {
        try {
            String resp = doRequest("GET", "/game", null, authToken);

            if (lastStatusCode == 200) {
                ListGamesResult r = gson.fromJson(resp, ListGamesResult.class);
                if (r.getGames() == null) r.games = new GameInfo[0];
                return r;
            }

            ErrorResponse err = gson.fromJson(resp, ErrorResponse.class);
            ListGamesResult r = new ListGamesResult();
            r.message = err == null ? resp : err.getMessage();
            r.games = new GameInfo[0];
            return r;

        } catch (IOException e) {
            ListGamesResult r = new ListGamesResult();
            r.message = e.getMessage(); // CLEAN error
            r.games = new GameInfo[0];
            lastStatusCode = 500;
            return r;
        }
    }

    public GenericResult joinGame(String playerColor, Integer gameID, String authToken) {
        var req = new SimpleJson();
        req.put("playerColor", playerColor);
        req.put("gameID", gameID);
        return doGenericRequest("PUT", "/game", gson.toJson(req), authToken);
    }

    public GenericResult clear() {
        return doGenericRequest("DELETE", "/db", null, null);
    }

    private GenericResult doGenericRequest(String method, String path, String body, String authToken) {
        try {
            String resp = doRequest(method, path, body, authToken);
            GenericResult r = new GenericResult();

            if (lastStatusCode == 200) {
                r.message = null;
            } else {
                ErrorResponse err = gson.fromJson(resp, ErrorResponse.class);
                r.message = err == null ? resp : err.getMessage();
            }
            return r;

        } catch (IOException e) {
            GenericResult r = new GenericResult();
            r.message = e.getMessage(); // CLEAN error
            lastStatusCode = 500;
            return r;
        }
    }

    // ===== low-level HTTP =====

    private String doRequest(String method, String path, String body, String authHeader) throws IOException {

        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setDoInput(true);

        if (authHeader != null) {
            conn.setRequestProperty("authorization", authHeader);
        }

        if (body != null) {
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }

        lastStatusCode = conn.getResponseCode();

        InputStream is = (lastStatusCode >= 200 && lastStatusCode < 400)
                ? conn.getInputStream()
                : conn.getErrorStream();

        if (is == null) return "";

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            return sb.toString();

        } finally {
            conn.disconnect();
        }
    }

    private static void setFieldIfExists(Object obj, String name, String value) {
        try {
            var f = obj.getClass().getDeclaredField(name);
            boolean a = f.canAccess(obj);
            if (!a) f.setAccessible(true);
            f.set(obj, value);
            if (!a) f.setAccessible(false);
        } catch (Exception ignored) {}
    }

    private static class SimpleJson extends java.util.HashMap<String, Object> {
        @Override
        public String toString() {
            return super.toString();
        }
    }
}
