package com.greenproject.classway.security;

import com.greenproject.classway.entity.User;
import com.greenproject.classway.exceptions.ResourceNotFoundException;
import com.greenproject.classway.global.UserState;
import com.greenproject.classway.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 유저 네임을 전달 받아 DB 내에 존재하는지 확인한다.
        Optional<User> optionalUser = userRepository.findByEmail(email);

        // 해당 유저를 찾을 수 없는 경우
        if (optionalUser.isEmpty()) {
            throw new UsernameNotFoundException("해당 이메일을 가진 사용자를 찾을 수 없습니다: " + email);
        }

        User user = optionalUser.get();

        if (user.getStatus() != UserState.ACTIVE) {
            throw new UsernameNotFoundException("사용할 수 없는 계정입니다.");
        }

        return org.springframework.security.core.userdetails.User
                .builder()
                .username(user.getEmail()) // 시큐리티가 식별할 수 있도록 이메일을 저장
                .password(user.getPassword())
                .roles(user.getRole().name()) // UserRole(STUDENT, TUTOR, ADMIN 등) 권한 매핑
                .build();
    }
}
