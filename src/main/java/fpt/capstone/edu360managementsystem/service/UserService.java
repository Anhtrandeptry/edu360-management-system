package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.response.RoomResponse;
import fpt.capstone.edu360managementsystem.dto.response.UserInfoResponse;
import fpt.capstone.edu360managementsystem.dto.response.UserResponse;
import fpt.capstone.edu360managementsystem.mapper.UserMapper;
import fpt.capstone.edu360managementsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }
}
