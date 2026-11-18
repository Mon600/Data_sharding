package app.domain;

import app.datalayer.model.OauthEntity;
import app.datalayer.model.UserEntity;
import app.datalayer.repository.OauthRepository;
import app.datalayer.repository.UserRepository;
import app.domain.model.*;
import app.exceptions.UserAlreadyExistsException;
import app.exceptions.UserNotFoundException;
import app.util.ContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.rmi.ServerException;
import java.security.GeneralSecurityException;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class UserService {
    private final PasswordService passwordService;

    private final UserRepository userRepository;

    private final OauthRepository oauthRepository;

    private final TokenService tokenService;

    private final OauthService oauthService;



    @Transactional(rollbackFor = Exception.class)
    public TokenDto createUser(UserDto user) {
        if (userRepository.existsByEmail(user.email()) || userRepository.existsByPhone(user.phone())) {
            throw new UserAlreadyExistsException("Registration failed. Use another email or phone.");
        }
        String hashedPassword = passwordService.hashPassword(user.password());
        UserEntity newUser = UserEntity.builder()
            .email(user.email())
            .name(user.name())
            .surname(user.surname())
            .phone(user.phone())
            .password(hashedPassword)
            .build();
        userRepository.save(newUser);
        String token = tokenService.generateToken(newUser.getId(), newUser.getEmail());
        return new TokenDto("Bearer", token);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public TokenDto loginUser(CredentialsDto credentials) {
        Optional<UserEntity> userOpt = userRepository.findByEmail(credentials.email());
        String passwordToCheck = userOpt.map(UserEntity::getPassword).orElseGet(
            () -> passwordService.hashPassword("123")
        );
        boolean passwordCheck = passwordService.verifyPassword(credentials.password(), passwordToCheck);
        if (userOpt.isEmpty() || !passwordCheck) {
            throw new UserNotFoundException("Login failed. Check your email and password.");
        }
        UserEntity user = userOpt.get();
        String token = tokenService.generateToken(user.getId(), user.getEmail());
        return new TokenDto("Bearer", token);
    }

    @Transactional(rollbackFor = Exception.class)
    public TokenDto changePassword(ChangePasswordDto changePasswordDto) {
        UUID userId = UUID.fromString(ContextHelper.getUserIdFromContext());
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found."));
        if (!passwordService.verifyPassword(changePasswordDto.oldPassword(), user.getPassword())) {
            throw new UserNotFoundException("Change password failed. Check your old password.");
        }

        String hashedPassword = passwordService.hashPassword(changePasswordDto.newPassword());
        user.setPassword(hashedPassword);
        userRepository.save(user);
        String token = tokenService.generateToken(user.getId(), user.getEmail());
        return new TokenDto("Bearer", token);
    }

    @Transactional(rollbackFor = Exception.class)
    public ResetPasswordResponseDto resetPassword(ResetPasswordDto resetPasswordDto) {
        String message = String.format("If the email %s is exists, a password reset link will be sent to you.",
            resetPasswordDto.email());

        Optional<UserEntity> userOpt = userRepository.findByEmail(resetPasswordDto.email());
        if (userOpt.isPresent()) {
            String newPassword = passwordService.generatePassword();

            System.out.println("New password: " + newPassword);
            UserEntity user = userOpt.get();
            user.setPassword(passwordService.hashPassword(newPassword));
            userRepository.save(user);
            return new ResetPasswordResponseDto(message);
        } else {
            String newPassword = passwordService.generatePassword();
            passwordService.hashPassword(newPassword);
        }
        return new ResetPasswordResponseDto(message);
    }

    public TokenDto loginViaGoogle(GoogleOauthDto googleOauthDto) {
        try {
            GoogleDataDto googleDataDto = oauthService.getDataFromGoogle(googleOauthDto);
            return processGoogleData(googleDataDto);
        } catch (GeneralSecurityException | IOException e) {
            throw new SecurityException("Failed to login via Google", e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public TokenDto processGoogleData(GoogleDataDto googleDataDto){
        Optional<OauthEntity> oauthEntityOpt = oauthRepository.findByProviderAndProviderUserId(
                ProviderEnum.GOOGLE,
                googleDataDto.googleId()
        );
        UserEntity user;
        if (oauthEntityOpt.isPresent()) {
            user = oauthEntityOpt.get().getUser();
        } else {
            Optional<UserEntity> userOpt = userRepository.findByEmail(googleDataDto.email());
            if (userOpt.isPresent()) {
                user = userOpt.get();
            } else {
                user = UserEntity.builder()
                        .name(googleDataDto.name())
                        .surname(googleDataDto.surname())
                        .email(googleDataDto.email())
                        .build();
                userRepository.save(user);
            }

            OauthEntity oauthEntity = OauthEntity.builder()
                    .user(user)
                    .provider(ProviderEnum.GOOGLE)
                    .providerUserId(googleDataDto.googleId())
                    .build();
            oauthRepository.save(oauthEntity);
        }
        return new TokenDto("Bearer", tokenService.generateToken(user.getId(), user.getEmail()));
    }
}
