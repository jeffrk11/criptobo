package com.jeff.cripto.exceptions;

import java.sql.SQLException;

public class DataBaseException extends RuntimeException{

    public DataBaseException(Exception e) {
        super(e);
    }

    public DataBaseException(SQLException e, String msg) {
        super(msg, e);
    }
}
