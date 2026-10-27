package com.company.pms.service.impl;

import com.company.pms.dto.request.CreateCommentRequest;
import com.company.pms.dto.request.UpdateCommentRequest;
import com.company.pms.dto.response.CommentResponse;
import com.company.pms.entity.Comment;
import com.company.pms.entity.Project;
import com.company.pms.entity.Task;
import com.company.pms.entity.User;
import com.company.pms.enums.ProjectStatus;
import com.company.pms.exception.BusinessRuleException;
import com.company.pms.exception.ResourceNotFoundException;
import com.company.pms.mapper.CommentMapper;
import com.company.pms.repository.CommentRepository;
import com.company.pms.repository.ProjectMemberRepository;
import com.company.pms.repository.TaskRepository;
import com.company.pms.repository.UserRepository;
import com.company.pms.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private CommentServiceImpl commentService;

    private User author;
    private Project project;
    private Task task;

    @BeforeEach
    void setUp() {
        author = TestData.user(1L, "Ann Lee");
        project = TestData.project(10L, ProjectStatus.ACTIVE, author);
        task = TestData.task(20L, project, author);
    }

    private static CommentResponse responseFor(Comment comment) {
        return new CommentResponse(comment.getId(), 20L, 1L, "Ann Lee",
                comment.getContent(), null, null);
    }

    private Task taskInClosedProject() {
        Project closed = TestData.project(10L, ProjectStatus.COMPLETED, author);
        return TestData.task(20L, closed, author);
    }

    // ---------- create ----------

    @Test
    void createIsBlockedInAClosedProject() {
        when(taskRepository.findById(20L)).thenReturn(Optional.of(taskInClosedProject()));

        assertThatThrownBy(() ->
                commentService.create(20L, new CreateCommentRequest(1L, "Hello")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("COMPLETED");

        verify(commentRepository, never()).save(any());
    }

    @Test
    void createRejectsAnUnknownAuthor() {
        when(taskRepository.findById(20L)).thenReturn(Optional.of(task));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                commentService.create(20L, new CreateCommentRequest(2L, "Hello")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Author not found with id 2");
    }

    @Test
    void createRejectsAnInactiveAuthor() {
        User inactive = TestData.user(2L, "Bob Ray");
        inactive.deactivate();
        when(taskRepository.findById(20L)).thenReturn(Optional.of(task));
        when(userRepository.findById(2L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() ->
                commentService.create(20L, new CreateCommentRequest(2L, "Hello")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Author is not an active user");
    }

    @Test
    void createRejectsAnAuthorWhoIsNotAMemberOfTheProject() {
        User outsider = TestData.user(2L, "Bob Ray");
        when(taskRepository.findById(20L)).thenReturn(Optional.of(task));
        when(userRepository.findById(2L)).thenReturn(Optional.of(outsider));
        when(projectMemberRepository.existsByProjectIdAndUserId(10L, 2L)).thenReturn(false);

        assertThatThrownBy(() ->
                commentService.create(20L, new CreateCommentRequest(2L, "Hello")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Author is not a member of this project");

        verify(commentRepository, never()).save(any());
    }

    @Test
    void createSavesTheTrimmedContentWithTheTaskAndAuthor() {
        Comment saved = TestData.withId(new Comment("Looks good", task, author), 30L);
        when(taskRepository.findById(20L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(projectMemberRepository.existsByProjectIdAndUserId(10L, 1L)).thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenReturn(saved);
        when(commentMapper.toResponse(saved)).thenReturn(responseFor(saved));

        commentService.create(20L, new CreateCommentRequest(1L, "  Looks good  "));

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("Looks good");
        assertThat(captor.getValue().getTask()).isSameAs(task);
        assertThat(captor.getValue().getAuthor()).isSameAs(author);
    }

    // ---------- list ----------

    @Test
    void listThrowsNotFoundForAMissingTaskWithoutQueryingComments() {
        when(taskRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> commentService.getByTask(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(commentRepository);
    }

    @Test
    void listKeepsTheOrderGivenByTheRepository() {
        Comment first = TestData.withId(new Comment("First", task, author), 30L);
        Comment second = TestData.withId(new Comment("Second", task, author), 31L);
        when(taskRepository.existsById(20L)).thenReturn(true);
        when(commentRepository.findByTaskIdOrderByCreatedAtAscIdAsc(20L))
                .thenReturn(List.of(first, second));
        when(commentMapper.toResponse(first)).thenReturn(responseFor(first));
        when(commentMapper.toResponse(second)).thenReturn(responseFor(second));

        List<CommentResponse> result = commentService.getByTask(20L);

        assertThat(result).extracting(CommentResponse::content).containsExactly("First", "Second");
    }

    // ---------- update ----------

    @Test
    void updateTrimsAndSavesTheNewContent() {
        Comment comment = TestData.withId(new Comment("Old", task, author), 30L);
        when(commentRepository.findById(30L)).thenReturn(Optional.of(comment));
        when(commentRepository.saveAndFlush(comment)).thenReturn(comment);
        when(commentMapper.toResponse(comment)).thenReturn(responseFor(comment));

        commentService.update(30L, new UpdateCommentRequest("  New text  "));

        assertThat(comment.getContent()).isEqualTo("New text");
    }

    @Test
    void updateIsBlockedInAClosedProject() {
        Comment comment = TestData.withId(new Comment("Old", taskInClosedProject(), author), 30L);
        when(commentRepository.findById(30L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.update(30L, new UpdateCommentRequest("New")))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(comment.getContent()).isEqualTo("Old");
        verify(commentRepository, never()).saveAndFlush(any());
    }

    // ---------- delete ----------

    @Test
    void deleteRemovesTheComment() {
        Comment comment = TestData.withId(new Comment("Bye", task, author), 30L);
        when(commentRepository.findById(30L)).thenReturn(Optional.of(comment));

        commentService.delete(30L);

        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteIsBlockedInAClosedProject() {
        Comment comment = TestData.withId(new Comment("Bye", taskInClosedProject(), author), 30L);
        when(commentRepository.findById(30L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.delete(30L))
                .isInstanceOf(BusinessRuleException.class);

        verify(commentRepository, never()).delete(any());
    }
}