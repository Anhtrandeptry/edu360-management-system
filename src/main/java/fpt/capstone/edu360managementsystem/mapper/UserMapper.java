package fpt.capstone.edu360managementsystem.mapper;

import fpt.capstone.edu360managementsystem.dto.response.UserInfoResponse;
import fpt.capstone.edu360managementsystem.dto.response.UserResponse;
import fpt.capstone.edu360managementsystem.entity.Role;
import fpt.capstone.edu360managementsystem.entity.User;
import fpt.capstone.edu360managementsystem.enums.ERole;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    UserResponse toResponse(User entity);

    default List<String> map(Set<Role> roles) {
        if (roles == null) return null;
        return roles.stream()
                .map(role -> role.getName().name()) // nếu Role.getName() trả về Enum ERole
                .collect(Collectors.toList());
    }
}
