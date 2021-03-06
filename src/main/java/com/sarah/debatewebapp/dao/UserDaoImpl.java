/*
 *  Copyright 2017 SarahBoka
 */

package com.sarah.debatewebapp.dao;

import com.sarah.debatewebapp.dto.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/*
 * @author Sarah
 */

public class UserDaoImpl implements UserDao {
    
    private JdbcTemplate jdbcTemp;
    
    @Override
    public void setJdbcTemplate(JdbcTemplate jdbcT){
        this.jdbcTemp = jdbcT;
    }
    
    private static final String SQL_ADD_USER = "INSERT INTO users (username, password, first_name, last_name, email, enabled)\n" +
"	VALUE (?, ?, ?, ?, ?, ?)";
    private static final String SQL_ADD_USER_AUTHORITY = "INSERT INTO authorities (username, authority)\n" +
"	VALUE (?, ?)";
    //CREATE USER
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    @Override
    public User createUser(User user){
        //for debugging
        String userN = user.getUsername();
        String passW = user.getPassword();
        String firstN = user.getFirstName();
        String lastN = user.getLastName();
        String email = user.getEmail();
        String role = user.getRole();
        jdbcTemp.update(SQL_ADD_USER,
                userN,
                passW,
                firstN,
                lastN,
                email,    
                1 //enabled auto set
        );
        int id = jdbcTemp.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
        user.setId(id);
        
        jdbcTemp.update(SQL_ADD_USER_AUTHORITY,
                user.getUsername(),
                role
        );
        return user;
    }
    
    private static final String SQL_GET_USERNAME = "SELECT username FROM users WHERE user_id = ?";
    private static final String SQL_DELETE_ATHORITY = "DELETE FROM authorities WHERE username = ?";
    private static final String SQL_UPDATE_USER = "UPDATE users SET username=?, first_name=?, last_name=?, email=?, wins=?, ties=?, losses=?, enabled=? \n" +
"	WHERE user_id=?";
    
    //UPDATE USER ... key constraints on username require deletion of child authorities, 
    //               update of user, and re-insertion of authorities
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    @Override
    public void updateUser(User user){
        //getting original username by id just in case username was changed in new obj
        String name = jdbcTemp.queryForObject(SQL_GET_USERNAME, String.class, user.getId());
        jdbcTemp.update(SQL_DELETE_ATHORITY, name);
        jdbcTemp.update(SQL_UPDATE_USER,
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getWins(),
                user.getTies(),
                user.getLosses(),
                user.isEnabled(),
                user.getId()
        );
        jdbcTemp.update(SQL_ADD_USER_AUTHORITY,
                user.getUsername(),
                user.getRole()
        );
    }
    
    private static final String SQL_DEACTIVATE_USER = "UPDATE users SET enabled=? \n" +
"	WHERE user_id=?";
    //DEACTIVATE USER
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    @Override
    public void deactivateUser(User user){
        
        jdbcTemp.update(SQL_DELETE_ATHORITY, user.getUsername());
        jdbcTemp.update(SQL_DEACTIVATE_USER,
                0,
                user.getId()
        );
        jdbcTemp.update(SQL_ADD_USER_AUTHORITY,
                user.getUsername(),
                user.getRole()
        );
    }
    
    private static final String SQL_CHANGE_AUTHORITY = "UPDATE authorities SET authority = ? WHERE username=?";
    //CHANGE USER AUTHORITY
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    @Override
    public void changeAuthority(User user){
        
        jdbcTemp.update(SQL_CHANGE_AUTHORITY,
                "ROLE_ADMIN",
                user.getUsername()
        );
    }
    
    private static final String SQL_GET_USER_BY_ID = "SELECT user_id AS id, users.username, password, first_name, last_name, email, wins, ties, losses, authority, enabled FROM users \n" +
"	LEFT JOIN authorities ON users.username = authorities.username \n" +
"    WHERE user_id = ?";
    //GET USER BY ID
    @Override
    public User getUserById(int id){
        try{
            return jdbcTemp.queryForObject(SQL_GET_USER_BY_ID, new UserMapper(), id);
        }catch(EmptyResultDataAccessException e){
            return null;
        }
    }
    
    private static final String SQL_GET_USER_BY_USERNAME = "SELECT user_id AS id, users.username, password, first_name, last_name, email, wins, ties, losses, authority, enabled FROM users \n" +
"	LEFT JOIN authorities ON users.username = authorities.username \n" +
"    WHERE users.username = ?";
    //GET USER BY USERNAME
    @Override
    public User getUserByUsername(String username){
        try{
            return jdbcTemp.queryForObject(SQL_GET_USER_BY_USERNAME, new UserMapper(), username);
        } catch(EmptyResultDataAccessException e){
            return null;
        }
    }
    
    private static final String SQL_GET_ALL_USERS = "SELECT user_id AS id, users.username, password, first_name, last_name, email, wins, ties, losses, authority, enabled FROM users \n" +
"	LEFT JOIN authorities ON users.username = authorities.username";
    //GET ALL USERS
    @Override
    public List<User> getAllUsers(){
        List<User> allUsers = jdbcTemp.query(SQL_GET_ALL_USERS, new UserMapper());
        return allUsers; 
    }
    
    private static final String SQL_DELETE_USER = "DELETE FROM users WHERE user_id = ?";
    //DELETE USER
    @Override
    public void deleteUser(int id){
        try {
            String name = jdbcTemp.queryForObject(SQL_GET_USERNAME, String.class, id);
            jdbcTemp.update(SQL_DELETE_ATHORITY, name);
            jdbcTemp.update(SQL_DELETE_USER, id);
        } catch (EmptyResultDataAccessException e){
            Logger.getLogger(UserDaoImpl.class.getName()).log(Level.WARNING, null, e);
        }
    }
    
    private static final class UserMapper implements RowMapper<User>{
        
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException{
            User user;
            int id = rs.getInt("id");
            String userN = rs.getString("username");
            String passW = rs.getString("password");
            String first = rs.getString("first_name");
            String last = rs.getString("last_name");
            String email = rs.getString("email");
            int wins = rs.getInt("wins");
            int ties = rs.getInt("ties");
            int totalD = rs.getInt("losses");
            String role = rs.getString("authority");
            Boolean enabled = rs.getBoolean("enabled");
            
            user = new User(id, userN, passW, first, last, email, wins, ties, totalD, role, enabled);
            
            return user;
        }
    }
}
