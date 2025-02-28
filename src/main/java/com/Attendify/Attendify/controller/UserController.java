package com.Attendify.Attendify.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Attendify.Attendify.dto.ResponseDTO;
import com.Attendify.Attendify.exception.UserNotFoundException;
import com.Attendify.Attendify.exception.UsernameAlreadyTakenException;
import com.Attendify.Attendify.service.UserService;
import com.Attendify.Attendify.user.User;


@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ResponseDTO> registerUser(@RequestBody User user) {
        try {
            ResponseDTO response = userService.registerUser(user);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (UsernameAlreadyTakenException e) {
            return new ResponseEntity<>(new ResponseDTO(e.getMessage(), false), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseDTO> loginUser(@RequestBody User user) {
        try {
            ResponseDTO response = userService.loginUser(user.getUsername(), user.getPassword());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (UserNotFoundException | IllegalArgumentException e) {
            return new ResponseEntity<>(new ResponseDTO(e.getMessage(), false), HttpStatus.BAD_REQUEST);
        }
    }
}
