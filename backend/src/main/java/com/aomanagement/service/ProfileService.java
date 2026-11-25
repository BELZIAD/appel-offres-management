package com.aomanagement.service;

import com.aomanagement.dto.SettingsDtos;
import com.aomanagement.entity.User;
import com.aomanagement.entity.UserPreferences;
import com.aomanagement.exception.NotFoundException;
import com.aomanagement.repository.UserPreferencesRepository;
import com.aomanagement.repository.UserRepository;
import com.aomanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final UserRepository userRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final AssetStorageService assetStorageService;

    @Transactional
    public SettingsDtos.ProfileResponse me(UserPrincipal principal) {
        return toProfile(user(principal));
    }

    @Transactional
    public SettingsDtos.ProfileResponse update(SettingsDtos.UserProfileRequest request, UserPrincipal principal) {
        User user = user(principal);
        applyProfile(user, request);
        return toProfile(user);
    }

    @Transactional
    public SettingsDtos.ProfileResponse preferences(SettingsDtos.UserPreferencesRequest request, UserPrincipal principal) {
        User user = user(principal);
        applyPreferences(preferences(user), request);
        return toProfile(user);
    }

    @Transactional
    public SettingsDtos.ProfileResponse updateFull(SettingsDtos.ProfileUpdateRequest request, UserPrincipal principal) {
        User user = user(principal);
        if (request.profile() != null) {
            applyProfile(user, request.profile());
        }
        if (request.preferences() != null) {
            applyPreferences(preferences(user), request.preferences());
        }
        return toProfile(user);
    }

    @Transactional
    public SettingsDtos.UploadResponse upload(String type, MultipartFile file, UserPrincipal principal) {
        User user = user(principal);
        String previous;
        if ("photo".equals(type)) previous = user.getPhoto();
        else if ("signature".equals(type)) previous = user.getSignature();
        else throw new NotFoundException("Type asset introuvable");
        String url = assetStorageService.store("profiles/" + user.getId(), file);
        if ("photo".equals(type)) user.setPhoto(url);
        else if ("signature".equals(type)) user.setSignature(url);
        assetStorageService.delete(previous);
        return new SettingsDtos.UploadResponse(url);
    }

    @Transactional
    public SettingsDtos.ProfileResponse deleteAsset(String type, UserPrincipal principal) {
        User user = user(principal);
        String previous;
        if ("photo".equals(type)) {
            previous = user.getPhoto();
            user.setPhoto(null);
        } else if ("signature".equals(type)) {
            previous = user.getSignature();
            user.setSignature(null);
        } else {
            throw new NotFoundException("Type asset introuvable");
        }
        assetStorageService.delete(previous);
        return toProfile(user);
    }

    private User user(UserPrincipal principal) {
        return userRepository.findById(principal.getId()).orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
    }

    private UserPreferences preferences(User user) {
        return preferencesRepository.findByUserId(user.getId()).orElseGet(() -> {
            UserPreferences prefs = new UserPreferences();
            prefs.setUser(user);
            return preferencesRepository.save(prefs);
        });
    }

    private void applyProfile(User user, SettingsDtos.UserProfileRequest request) {
        if (request.firstName() != null) user.setFirstName(request.firstName());
        if (request.lastName() != null) user.setLastName(request.lastName());
        user.setJobTitle(request.jobTitle());
        user.setDepartment(request.department());
        user.setPhone(request.phone());
        if (request.language() != null) user.setLanguage(request.language());
        if (request.timezone() != null) user.setTimezone(request.timezone());
    }

    private void applyPreferences(UserPreferences prefs, SettingsDtos.UserPreferencesRequest request) {
        prefs.setTheme(request.theme() == null ? "SYSTEM" : request.theme());
        prefs.setLanguage(request.language() == null ? "fr" : request.language());
        prefs.setEmailNotifications(request.emailNotifications());
        prefs.setBrowserNotifications(request.browserNotifications());
        prefs.setTimezone(request.timezone() == null ? "Africa/Casablanca" : request.timezone());
        prefs.setDateFormat(request.dateFormat() == null ? "dd/MM/yyyy" : request.dateFormat());
        prefs.setTimeFormat(request.timeFormat() == null ? "HH:mm" : request.timeFormat());
        prefs.setPageSize(request.pageSize() <= 0 ? 20 : request.pageSize());
    }

    private SettingsDtos.ProfileResponse toProfile(User user) {
        UserPreferences p = preferences(user);
        SettingsDtos.UserPreferencesResponse prefs = new SettingsDtos.UserPreferencesResponse(p.getTheme(), p.getLanguage(), p.isEmailNotifications(), p.isBrowserNotifications(), p.getTimezone(), p.getDateFormat(), p.getTimeFormat(), p.getPageSize());
        return new SettingsDtos.ProfileResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getRole().name(), user.getPhoto(), user.getSignature(), user.getJobTitle(), user.getDepartment(), user.getPhone(), user.getLanguage(), user.getTimezone(), prefs);
    }
}
