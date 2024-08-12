package ru.ksergey.todolist.dao;


import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import ru.ksergey.todolist.dto.UpdateToDoDto;
import ru.ksergey.todolist.exceptions.ToDoNoExistException;
import ru.ksergey.todolist.model.TodoItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.List;
import java.sql.ResultSet;
import java.util.Objects;

@Repository
public class TodoItemDaoImpl implements TodoItemDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<TodoItem> todoItemRowMapper = (ResultSet rs, int rowNum) -> {
        TodoItem todoItem = new TodoItem();
        todoItem.setId(rs.getLong("id"));
        todoItem.setTitle(rs.getString("title"));
        todoItem.setDescription(rs.getString("description"));
        todoItem.setStatus(TodoItem.TodoStatus.valueOf(rs.getString("status")));
        todoItem.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return todoItem;
    };

    @Override
    public List<TodoItem> getAllTodoItems() {
        String sql = "SELECT * FROM todo_items ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, todoItemRowMapper);
    }

    @Override
    public TodoItem getTodoItemById(Long id) {
        String sql = "SELECT * FROM todo_items WHERE id = ?";
        TodoItem todoItem;

        try {
            todoItem = jdbcTemplate.queryForObject(sql, todoItemRowMapper, id);
        } catch (RuntimeException e) {
            throw new ToDoNoExistException("No such ToDo exist.");
        }
        return todoItem;
    }


    @Override
    public TodoItem createTodoItem(TodoItem todoItem) {
        String sql = "INSERT INTO todo_items (title, description, status) VALUES (?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, todoItem.getTitle());
            ps.setString(2, todoItem.getDescription());
            ps.setString(3, todoItem.getStatus().name());
            return ps;
        }, keyHolder);

        Long id = Objects.requireNonNull(keyHolder.getKey()).longValue();
        todoItem.setId(id);

        String selectSql = "SELECT * FROM todo_items WHERE id = ?";
        return jdbcTemplate.queryForObject(selectSql, todoItemRowMapper, id);
    }

    @Override
    public TodoItem updateTodoItem(UpdateToDoDto todoItem, Long id) {
        String sql = "UPDATE todo_items SET title = ?, description = ?, status = ? WHERE id = ?";

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, todoItem.getTitle());
            ps.setString(2, todoItem.getDescription());
            ps.setString(3, todoItem.getStatus().name());
            ps.setString(4, id.toString());
            return ps;
        });

        try {
            String selectSql = "SELECT * FROM todo_items WHERE id = ?";
            return jdbcTemplate.queryForObject(selectSql, todoItemRowMapper, id);
        } catch (IncorrectResultSizeDataAccessException e) {
            throw new ToDoNoExistException("No such ToDo exist.");
        }
    }
}
