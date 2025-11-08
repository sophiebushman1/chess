package client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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


    public static class AuthResult {
        private String authToken;
        private String username;
        private String message; // for error responses

        public String getAuthToken() {
            return authToken;
        }

        public String getUsername() {
            return username;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class CreateGameResult {
        private Integer gameID;
        private String message;

        public Integer getGameID() {
            return gameID;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class GameInfo {
        private int gameID;
        private String whiteUsername;
        private String blackUsername;
        private String gameName;

        public int getGameID() {
            return gameID;
        }

        public String getWhiteUsername() {
            return whiteUsername;
        }

        public String getBlackUsername() {
            return blackUsername;
        }

        public String getGameName() {
            return gameName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GameInfo)) return false;
            GameInfo g = (GameInfo) o;
            return gameID == g.gameID
                    && ((whiteUsername == null && g.whiteUsername == null) || (whiteUsername != null && whiteUsername.equals(g.whiteUsername)))
                    && ((blackUsername == null && g.blackUsername == null) || (blackUsername != null && blackUsername.equals(g.blackUsername)))
                    && ((gameName == null && g.gameName == null) || (gameName != null && gameName.equals(g.gameName)));
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(gameID) ^ (whiteUsername == null ? 0 : whiteUsername.hashCode()) ^ (blackUsername == null ? 0 : blackUsername.hashCode());
        }
    }

    public static class ListGamesResult {
        private GameInfo[] games;
        private String message;

        public GameInfo[] getGames() {
            return games;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class GenericResult {
        private String message;

        public String getMessage() {
            return message;
        }
    }


    public AuthResult register(String username, String password, String email) {
        var reqMap = new SimpleJson();
        reqMap.put("username", username);
        reqMap.put("password", password);
        reqMap.put("email", email);

        try {
            String resp = doRequest("POST", "/user", gson.toJson(reqMap), null);
            if (lastStatusCode == 200) {
                AuthResult r = gson.fromJson(resp, AuthResult.class);
                return r;
            } else {

                AuthResult r = new AuthResult();
                ErrorResponse err = gson.fromJson(resp, ErrorResponse.class);
                r.message = err == null ? resp : err.getMessage();
                return r;
            }
        } catch (IOException e) {
            AuthResult r = new AuthResult();
            r.message = "Error: " + e.getMessage();
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
                AuthResult r = gson.fromJson(resp, AuthResult.class);
                return r;
            } else {
                AuthResult r = new AuthResult();
                ErrorResponse err = gson.fromJson(resp, ErrorResponse.class);
                r.message = err == null ? resp : err.getMessage();
                return r;
            }
        } catch (IOException e) {
            AuthResult r = new AuthResult();
            r.message = "Error: " + e.getMessage();
            lastStatusCode = 500;
            return r;
        }
    }


    public GenericResult logout(String authToken) {
        try {
            String resp = doRequest("DELETE", "/session", null, authToken);
            GenericResult r = new GenericResult();
            if (lastStatusCode == 200) {
                r.message = null; // success has no message
            } else {
                ErrorResponse err = gson.fromJson(resp, ErrorResponse.class);
                r.message = err == null ? resp : err.getMessage();
            }
            return r;
        } catch (IOException e) {
            GenericResult r = new GenericResult();
            r.message = "Error: " + e.getMessage();
            lastStatusCode = 500;
            return r;
        }
    }

    /**
     * POST /game
     */
    public CreateGameResult createGame(String gameName, String authToken) {
        var req = new SimpleJson();
        req.put("gameName", gameName);
        try {
            String resp = doRequest("POST", "/game", gson.toJson(req), authToken);
            CreateGameResult r;
            if (lastStatusCode == 200) {
                r = gson.fromJson(resp, CreateGameResult.class);
            } else {
                r = new CreateGameResult();
                ErrorResponse err = gson.fromJson(resp, ErrorResponse.class);

                setFieldIfExists(r, "message", err == null ? resp : err.getMessage());
            }
            return r;
        } catch (IOException e) {
            CreateGameResult r = new CreateGameResult();
            setFieldIfExists(r, "message", "Error: " + e.getMessage());
            lastStatusCode = 500;
            return r;
        }
    }

    /**
     * GET /game
     */
    public ListGamesResult listGames(String authToken) {
        try {
            String resp = doRequest("GET", "/game", null, authToken);
            if (lastStatusCode == 200) {

                ListGamesResult r = gson.fromJson(resp, ListGamesResult.class);
                // non-null games
                if (r.games == null) r.games = new GameInfo[0];
                return r;
            } else {
                ListGamesResult r = new ListGamesResult();
                ErrorResponse err = gson.fromJson(resp, ErrorResponse.class);
                setFieldIfExists(r, "message", err == null ? resp : err.getMessage());
                r.games = new GameInfo[0];
                return r;
            }
        } catch (IOException e) {
            ListGamesResult r = new ListGamesResult();
            setFieldIfExists(r, "message", "Error: " + e.getMessage());
            r.games = new GameInfo[0];
            lastStatusCode = 500;
            return r;
        }
    }


    public GenericResult joinGame(String playerColor, Integer gameID, String authToken) {
        var req = new SimpleJson();
        req.put("playerColor", playerColor);
        req.put("gameID", gameID);
        try {
            String resp = doRequest("PUT", "/game", gson.toJson(req), authToken);
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
            r.message = "Error: " + e.getMessage();
            lastStatusCode = 500;
            return r;
        }
    }


    public GenericResult clear() {
        try {
            String resp = doRequest("DELETE", "/db", null, null);
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
            r.message = "Error: " + e.getMessage();
            lastStatusCode = 500;
            return r;
        }
    }


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
                byte[] input = body.getBytes(StandardCharsets.UTF_8);
                os.write(input);
                os.flush();
            }
        }

        // trigger the request
        int code = conn.getResponseCode();
        lastStatusCode = code;

        InputStream is = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) return "";

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    //helper
    private static void setFieldIfExists(Object obj, String fieldName, String value) {
        try {
            var f = obj.getClass().getDeclaredField(fieldName);
            boolean accessible = f.canAccess(obj);
            if (!accessible) f.setAccessible(true);
            f.set(obj, value);
            if (!accessible) f.setAccessible(false);
        } catch (Exception ignored) {
        }
    }


    private static class ErrorResponse {
        private String message;

        public String getMessage() {
            return message;
        }
    }


    private static class SimpleJson extends java.util.HashMap<String, Object> {
        @Override
        public String toString() {
            return super.toString();
        }
    }
}
