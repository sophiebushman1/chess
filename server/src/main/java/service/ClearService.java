package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;

public class ClearService {
    private final DataAccess dataAccess;

    public ClearService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    /** Clears all data from the database. */
    public void clear() throws DataAccessException {
        dataAccess.clear();
    }
}