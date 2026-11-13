package com.mg.nmlonline.domain.model.press;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "GAZETTE_ISSUES")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GazetteIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gazette_issue_seq")
    @SequenceGenerator(name = "gazette_issue_seq", sequenceName = "gazette_issues_id_seq",
            allocationSize = 50)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Column(nullable = false)
    private int turn;

    @Column(name = "issue_number", nullable = false)
    private int issueNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GazetteStatus status;

    @Column(name = "main_headline", length = 255)
    private String mainHeadline;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "editor_notes", columnDefinition = "text")
    private String editorNotes;

    @Column(name = "final_edition", nullable = false)
    private boolean finalEdition;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "published_by_user_id")
    private Long publishedByUserId;
}
