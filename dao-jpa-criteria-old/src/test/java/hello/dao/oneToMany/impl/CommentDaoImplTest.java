package hello.dao.oneToMany.impl;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import hello.BaseTest;
import hello.container.FieldHolder;
import hello.dao.oneToMany.CommentDao;
import hello.entity.oneToMany.Comment;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@DatabaseSetup({"/post_comment.xml"})
public class CommentDaoImplTest extends BaseTest {

    @Autowired
    private CommentDao commentDao;

    @Test
    public void findByPostId() {
        List<Comment> comments = commentDao.findByPostId(1L);

        assertThat(comments.size(), equalTo(2));
        for(Comment comment : comments) {
            assertThat(comment.getPost().getId(), equalTo(1L));
        }
    }

    @Test
    public void getByFields_WhenExist() {
        ArrayList<FieldHolder> fields = new ArrayList<>();
        fields.add(FieldHolder.of("name", "Comment#1", false));
        fields.add(FieldHolder.of("post", 1L, true));
        List<Comment> comments = commentDao.getByFields(fields);

        assertThat(comments.size(), equalTo(1));
        assertThat(comments, containsInAnyOrder(
                hasProperty("name", is("Comment#1"))
        ));
    }
}