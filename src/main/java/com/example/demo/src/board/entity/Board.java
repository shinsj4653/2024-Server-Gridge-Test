package com.example.demo.src.board.entity;

import com.example.demo.common.entity.BaseEntity;
import com.example.demo.src.user.entity.User;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import javax.persistence.*;

import java.util.ArrayList;
import java.util.List;

import static com.example.demo.common.entity.BaseEntity.State.*;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.*;
import static javax.persistence.FetchType.*;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@JsonAutoDetect(fieldVisibility = ANY)
@Audited
@Table(name = "TB_BOARD")
public class Board extends BaseEntity {

    @Id
    @Column(name = "boardId", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 2200)
    private String content;

    @NotAudited
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "userId")
    private User user;

    @NotAudited
    @OneToMany(mappedBy = "board", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnore
    List<BoardImage> boardImgList = new ArrayList<>();

    public void setUser(User user){
        this.user = user;
    }

    @Builder
    public Board(Long id, String content, User user) {
        this.id = id;
        this.content = content;
        this.user = user;
    }

    public Long findUserId() {
        return user.getId();
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void updateState(State state) {
        this.state = state;
    }

    public void deleteFeed() {
        this.state = INACTIVE;
    }

    // Board에서 파일 처리 위함
    public void addImage(BoardImage image) {
        this.boardImgList.add(image);

        // 게시글에 파일이 저장되어있지 않은 경우
        if(image.getBoard() != this) {
            image.setBoard(this); // 파일 저장
        }

    }

}
