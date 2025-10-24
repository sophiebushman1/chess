package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;

/**
 * Service class responsible for clearing the entire application state (for testing/setup).
 */
public class ClearService {
    private final DataAccess dataAccess;

    public ClearService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    /**
     * Clears all data from the application (users, games, and auth tokens).
     */
    public void clearApplication() throws DataAccessException {
        dataAccess.clear();
    }
}