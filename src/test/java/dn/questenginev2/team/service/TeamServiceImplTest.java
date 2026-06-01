package dn.questenginev2.team.service;

import dn.questenginev2.common.exceptions.TeamAlreadyExistsException;
import dn.questenginev2.team.dto.CreateTeamRequest;
import dn.questenginev2.team.dto.TeamResponse;
import dn.questenginev2.team.entity.Team;
import dn.questenginev2.team.repository.TeamRepository;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceImplTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TeamServiceImpl teamService;

    private User testUser;
    private CreateTeamRequest createTeamRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPublicName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashedPassword");

        createTeamRequest = new CreateTeamRequest();
        createTeamRequest.setName("Test Team");
    }

    @Test
    void createTeam_createsTeam_whenNameIsUnique() {
        // Arrange
        when(authentication.getName()).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(teamRepository.existsByName("Test Team")).thenReturn(false);
        
        Team savedTeam = Team.builder()
                .id(1L)
                .name("Test Team")
                .captain(testUser)
                .createdAt(Instant.now())
                .build();
        when(teamRepository.save(any(Team.class))).thenReturn(savedTeam);

        // Act
        TeamResponse response = teamService.createTeam(createTeamRequest, authentication);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Test Team");
        assertThat(response.getCaptainName()).isEqualTo("Test User");
        assertThat(response.getCreatedAt()).isNotNull();

        verify(teamRepository).existsByName("Test Team");
        verify(teamRepository).save(any(Team.class));
        verify(userService).findByUsername("testuser");
    }

    @Test
    void createTeam_throwsTeamAlreadyExistsException_whenNameAlreadyExists() {
        // Arrange
        when(authentication.getName()).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(teamRepository.existsByName("Test Team")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> teamService.createTeam(createTeamRequest, authentication))
                .isInstanceOf(TeamAlreadyExistsException.class)
                .hasMessage("Team with name Test Team already exists");

        verify(teamRepository).existsByName("Test Team");
        verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    void createTeam_throwsRuntimeException_whenUserNotFound() {
        // Arrange
        when(authentication.getName()).thenReturn("nonexistent");
        when(userService.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> teamService.createTeam(createTeamRequest, authentication))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Пользователь не найден: nonexistent");

        verify(teamRepository, never()).existsByName(anyString());
        verify(teamRepository, never()).save(any(Team.class));
    }
}