package com.slilio.xiaohashu.comment.biz;

import com.slilio.xiaohashu.comment.biz.model.vo.FindCommentPageListReqVO;
import com.slilio.xiaohashu.comment.biz.service.impl.CommentServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @Author: slilio @CreateTime: 2025-06-18 @Description: 集成测试 @Version: 1.0
 */
@SpringBootTest
public class CommentServiceTest {
  @Autowired private CommentServiceImpl commentService;

  /** 测试评论分页查询 */
  @Test
  void printTest() {

    FindCommentPageListReqVO findCommentPageListReqVO =
        FindCommentPageListReqVO.builder().noteId(1862481582414102549L).pageNo(1).build();
    commentService.findCommentPageList(findCommentPageListReqVO);
  }
}
