package com.mg.nmlonline;

import com.mg.nmlonline.infrastructure.repository.BoardRepository;

public final class DemoFixtures {

    public static final String MAIN_BOARD_NAME = "Carte Principale";

    private DemoFixtures() {
    }

    public static Long mainBoardId(BoardRepository boardRepository) {
        return boardRepository.findByName(MAIN_BOARD_NAME)
                .orElseThrow(() -> new IllegalStateException("Plateau de démo principal introuvable"))
                .getId();
    }
}
