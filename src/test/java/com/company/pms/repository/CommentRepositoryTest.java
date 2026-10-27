package com.company.pms.repository;

import com.company.pms.entity.Comment;
import com.company.pms.entity.Project;
import com.company.pms.entity.Task;
import com.company.pms.entity.User;
import com.company.pms.enums.TaskStatus;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommentRepositoryTest extends RepositoryTestBase {

    private User ann;
    private User bob;
    private Project alpha;
    private Project beta;
    private Task alphaTask1;
    private Task alphaTask2;
    private Task betaTask;

    @BeforeEach
    void setUp() {
        ann = saveUser("Ann Lee");
        bob = saveUser("Bob Ray");
        alpha = saveProject("Alpha", ann);
        beta = saveProject("Beta", ann);
        alphaTask1 = saveTask(alpha, ann, null, TaskStatus.TODO, null);
        alphaTask2 = saveTask(alpha, ann, null, TaskStatus.TODO, null);
        betaTask = saveTask(beta, ann, null, TaskStatus.TODO, null);
    }

    private Comment saveComment(Task task, User author, String content) {
        return commentRepository.save(new Comment(content, task, author));
    }

    @Test
    void commentsComeOldestFirstWithTheirAuthorsAlreadyLoaded() {
        saveComment(alphaTask1, ann, "first");
        saveComment(alphaTask1, bob, "second");
        saveComment(alphaTask1, ann, "third");
        saveComment(alphaTask2, bob, "other task");
        flushAndClear();

        List<Comment> result =
                commentRepository.findByTaskIdOrderByCreatedAtAscIdAsc(alphaTask1.getId());

        assertThat(result).extracting(Comment::getContent)
                .containsExactly("first", "second", "third");
        assertThat(result).allSatisfy(comment ->
                assertThat(Hibernate.isInitialized(comment.getAuthor())).isTrue());
    }

    @Test
    void deleteByTaskIdRemovesOnlyThatTasksComments() {
        saveComment(alphaTask1, ann, "one");
        saveComment(alphaTask1, bob, "two");
        saveComment(alphaTask2, ann, "keep me");

        commentRepository.deleteByTaskId(alphaTask1.getId());

        assertThat(commentRepository.findByTaskIdOrderByCreatedAtAscIdAsc(alphaTask1.getId()))
                .isEmpty();
        assertThat(commentRepository.findByTaskIdOrderByCreatedAtAscIdAsc(alphaTask2.getId()))
                .hasSize(1);
    }

    @Test
    void deleteByProjectIdRemovesTheCommentsOfEveryTaskInThatProject() {
        saveComment(alphaTask1, ann, "a1");
        saveComment(alphaTask2, bob, "a2");
        saveComment(betaTask, ann, "keep me");

        commentRepository.deleteByProjectId(alpha.getId());

        assertThat(commentRepository.findByTaskIdOrderByCreatedAtAscIdAsc(alphaTask1.getId()))
                .isEmpty();
        assertThat(commentRepository.findByTaskIdOrderByCreatedAtAscIdAsc(alphaTask2.getId()))
                .isEmpty();
        assertThat(commentRepository.findByTaskIdOrderByCreatedAtAscIdAsc(betaTask.getId()))
                .hasSize(1);
    }
}