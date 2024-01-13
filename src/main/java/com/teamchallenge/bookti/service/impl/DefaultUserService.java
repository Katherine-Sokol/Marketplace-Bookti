package com.teamchallenge.bookti.service.impl;

import com.teamchallenge.bookti.dto.authorization.NewUserRegistrationRequest;
import com.teamchallenge.bookti.model.PasswordResetToken;
import com.teamchallenge.bookti.dto.user.UserInfo;
import com.teamchallenge.bookti.exception.PasswordIsNotMatchesException;
import com.teamchallenge.bookti.exception.PasswordResetTokenNotFoundException;
import com.teamchallenge.bookti.exception.UserAlreadyExistsException;
import com.teamchallenge.bookti.exception.UserNotFoundException;
import com.teamchallenge.bookti.mapper.AuthorizedUserMapper;
import com.teamchallenge.bookti.model.UserEntity;
import com.teamchallenge.bookti.repository.PasswordResetTokenRepository;
import com.teamchallenge.bookti.repository.UserRepository;
import com.teamchallenge.bookti.security.AuthorizedUser;
import com.teamchallenge.bookti.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class DefaultUserService implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordTokenRepository;

    @Override
    public AuthorizedUser create(NewUserRegistrationRequest userDetails) {
        if (!userDetails.getPassword().equals(userDetails.getConfirmPassword())) {
            throw new PasswordIsNotMatchesException("Password is not matches");
        }
        if (userRepository.existsUserByEmail(userDetails.getEmail())) {
            throw new UserAlreadyExistsException(MessageFormat.format("User with email <{0}> already exists", userDetails.getEmail()));
        }
        userDetails.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        UserEntity user = UserEntity.build(userDetails);
        userRepository.save(user);
        return AuthorizedUserMapper.mapFrom(user);
    }

    @Override
    public UserInfo findById(UUID id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(MessageFormat.format("User with id <{0}> not found.", id)));
        return UserInfo.mapFrom(user);
    }

    @Override
    public UserInfo findUserByEmail(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(MessageFormat.format("User with email <{0}> not found.", email)));
        return UserInfo.mapFrom(user);
    }

    @Override
    public void changeUserPassword(UUID userId, String newPassword) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(MessageFormat.format("User with id <{0}> not found.", userId)));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        passwordTokenRepository.deletePasswordResetTokenByUserId(user);
    }

    @Override
    public PasswordResetToken createPasswordResetTokenForUser(UserInfo user, String token) {
        UserEntity userEntity = userRepository.findById(user.getId())
                .orElseThrow(() -> new UserNotFoundException(MessageFormat.format("User with id <{0}> not found.", user.getId())));
        if (passwordTokenRepository.findByUser(userEntity) != null) {
            passwordTokenRepository.deletePasswordResetTokenByUserId(userEntity);
        }
        PasswordResetToken passwordResetToken = new PasswordResetToken(userEntity, token);
        return passwordTokenRepository.save(passwordResetToken);
    }

    @Override
    public PasswordResetToken getPasswordResetToken(String token) {
        return passwordTokenRepository.findByToken(token)
                .orElseThrow(() -> new PasswordResetTokenNotFoundException(MessageFormat.format("Password reset token <{0}> not found.", token)));
    }

    @Override
    public UserInfo getUserByPasswordResetToken(String passwordResetToken) {
        UserEntity user = getPasswordResetToken(passwordResetToken).getUser();
        return UserInfo.mapFrom(user);
    }
}
