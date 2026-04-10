# ============================================================================
# ProjetTutoreDOOM - Makefile
# ============================================================================

# --- Configuration ---
JAVAC       = javac
JAVA        = java
SRC_DIR     = src
TEST_DIR    = test
BIN_DIR     = bin
ASSETS_DIR  = assets

# Points d'entrée
MAIN_CLASS      = game.MainGameMultiplayer
RRT_CLASS       = monstre.MainRRT
RRT_PROG_CLASS  = monstre.MainRRTProgressif

# Trouver toutes les sources
SOURCES     = $(shell find $(SRC_DIR) -name "*.java")
TEST_SOURCES = $(shell find $(TEST_DIR) -name "*.java")

# Options de compilation
JAVAC_FLAGS = -d $(BIN_DIR) -sourcepath $(SRC_DIR)

# ============================================================================
# Cibles principales
# ============================================================================

.PHONY: all run run-rrt run-rrt-prog clean help

## Compile toutes les sources
all: $(BIN_DIR)
	$(JAVAC) $(JAVAC_FLAGS) $(SOURCES)

## Compile et lance le jeu multijoueur
run: all
	$(JAVA) -cp $(BIN_DIR) $(MAIN_CLASS)

## Crée le dossier de sortie
$(BIN_DIR):
	mkdir -p $(BIN_DIR)

## Supprime les fichiers compilés
clean:
	rm -rf $(BIN_DIR)

## Affiche l'aide
help:
	@echo ""
	@echo "  ProjetTutoreDOOM - Cibles disponibles"
	@echo "  ======================================"
	@echo ""
	@echo "  make            Compile toutes les sources"
	@echo "  make run        Compile et lance le jeu multijoueur"
	@echo "  make clean      Supprime les fichiers compiles"
	@echo "  make help       Affiche cette aide"
	@echo ""
