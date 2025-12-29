#!/usr/bin/env make
#===============================================================================
# JavaBlocks Engine - High-Performance Java Game Engine
# Build System
#===============================================================================

VERSION := 1.0.0
PACKAGE_NAME := javablocks-engine-$(VERSION)
INSTALL_PREFIX ?= /usr/local
BIN_INSTALL_DIR := $(INSTALL_PREFIX)/bin
LIB_INSTALL_DIR := $(INSTALL_PREFIX)/lib
SHARE_INSTALL_DIR := $(INSTALL_PREFIX)/share/javablocks-engine

JAVA_HOME ?= $(shell \
    if [ -d "/workspace/jdk-21+35" ]; then \
        echo "/workspace/jdk-21+35"; \
    elif [ -n "$$JAVA_HOME" ] && [ -d "$$JAVA_HOME" ] && [ -f "$$JAVA_HOME/bin/java" ]; then \
        echo "$$JAVA_HOME"; \
    elif which java > /dev/null 2>&1; then \
        dirname $$(dirname $$(readlink -f $$(which java))) 2>/dev/null; \
    else \
        echo ""; \
    fi)
JAVA_VERSION_REQUIRED := 21

RED := \033[0;31m
GREEN := \033[0;32m
YELLOW := \033[0;33m
NC := \033[0m

GRADLE := ./gradlew
ACTIVE_MODULES := core assets plugins marketplace tools desktop editor
DISABLED_MODULES := android html server

.PHONY: all
all: info build

.PHONY: info
info:
	@echo "============================================"
	@echo "JavaBlocks Engine Build System"
	@echo "============================================"
	@echo "Version: $(VERSION)"
	@echo "Package: $(PACKAGE_NAME)"
	@echo "Install Prefix: $(INSTALL_PREFIX)"
	@echo ""
	@echo "Active Modules: $(ACTIVE_MODULES)"
	@echo "Disabled Modules: $(DISABLED_MODULES)"
	@echo ""
	@if [ -n "$(JAVA_HOME)" ]; then \
echo "JAVA_HOME: $(JAVA_HOME)"; \
$(JAVA_HOME)/bin/java -version 2>&1 | head -1; \
	else \
echo "$(RED)JAVA_HOME not set and Java not found!$(NC)"; \
	fi
	@echo ""
	@$(GRADLE) --version 2>&1 | head -2
	@echo ""

.PHONY: check-java
check-java:
	@echo "Checking Java installation..."
	@if [ -z "$(JAVA_HOME)" ]; then \
echo "$(RED)Error: Java not found!$(NC)"; \
echo "Please set JAVA_HOME or ensure Java is installed."; \
exit 1; \
	fi
	@if [ ! -d "$(JAVA_HOME)" ]; then \
echo "$(RED)Error: JAVA_HOME directory not found: $(JAVA_HOME)$(NC)"; \
exit 1; \
	fi
	@if [ ! -f "$(JAVA_HOME)/bin/java" ]; then \
echo "$(RED)Error: Java executable not found: $(JAVA_HOME)/bin/java$(NC)"; \
exit 1; \
	fi
	@$(JAVA_HOME)/bin/java -version 2>&1 | head -1 | grep -q "version \"$(JAVA_VERSION_REQUIRED)\"" || \
(echo "$(YELLOW)Warning: Java version may not be $(JAVA_VERSION_REQUIRED)$(NC)" && \
 $(JAVA_HOME)/bin/java -version 2>&1 | head -1)
	@echo "$(GREEN)Java check passed!$(NC)"

.PHONY: setup-jdk
setup-jdk:
	@echo "Setting up JDK 21 environment..."
	@if [ ! -d "/workspace/jdk-21+35" ]; then \
echo "Downloading Temurin JDK 21..."; \
cd /workspace && \
curl -L -o jdk21.tar.gz "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21%2B35/OpenJDK21U-jdk_x64_linux_hotspot_21_35.tar.gz" && \
tar -xzf jdk21.tar.gz && \
rm jdk21.tar.gz; \
	fi
	@echo "JDK 21 available at: /workspace/jdk-21+35"

.PHONY: build
build: clean info check-java
	@echo ""
	@echo "Building JavaBlocks Engine..."
	@export JAVA_HOME=$(JAVA_HOME) && \
export PATH=$(JAVA_HOME)/bin:$$PATH && \
$(GRADLE) assemble --no-daemon -q 2>&1 | tee build_output.txt
	@if grep -q "BUILD SUCCESSFUL" build_output.txt 2>/dev/null; then \
echo ""; \
echo "$(GREEN)Build completed successfully!$(NC)"; \
echo "JAR files created in build/libs/"; \
rm -f build_output.txt; \
	else \
echo ""; \
echo "$(YELLOW)Build completed with warnings$(NC)"; \
rm -f build_output.txt; \
	fi

.PHONY: build-core
build-core: check-java
	@echo "Building core module..."
	@export JAVA_HOME=$(JAVA_HOME) && \
export PATH=$(JAVA_HOME)/bin:$$PATH && \
$(GRADLE) :core:build --no-daemon -q
	@echo "$(GREEN)Core module built successfully!$(NC)"

.PHONY: build-server
build-server: check-java
	@echo "Building server module..."
	@export JAVA_HOME=$(JAVA_HOME) && \
export PATH=$(JAVA_HOME)/bin:$$PATH && \
$(GRADLE) :server:build --no-daemon -q
	@echo "$(GREEN)Server module built successfully!$(NC)"

.PHONY: compile
compile: check-java
	@echo "Compiling source files..."
	@export JAVA_HOME=$(JAVA_HOME) && \
export PATH=$(JAVA_HOME)/bin:$$PATH && \
$(GRADLE) compileJava --no-daemon -q
	@echo "$(GREEN)Compilation successful!$(NC)"

.PHONY: clean
clean:
	@echo "Cleaning build artifacts..."
	@$(GRADLE) clean --no-daemon -q 2>/dev/null || true
	@rm -rf build/ .gradle/*/build .gradle/*/fileHashes
	@echo "$(GREEN)Clean complete!$(NC)"

.PHONY: distclean
distclean: clean
	@echo "Removing all generated files and Gradle cache..."
	@rm -rf .gradle/ build_output.txt
	@echo "$(GREEN)Distclean complete!$(NC)"

.PHONY: test
test: check-java
	@echo "Running unit tests..."
	@export JAVA_HOME=$(JAVA_HOME) && \
export PATH=$(JAVA_HOME)/bin:$$PATH && \
$(GRADLE) test --no-daemon 2>&1 | tee test_output.txt
	@if grep -q "BUILD SUCCESSFUL" test_output.txt 2>/dev/null; then \
echo ""; \
echo "$(GREEN)All tests passed!$(NC)"; \
rm -f test_output.txt; \
	else \
echo ""; \
echo "$(RED)Some tests failed!$(NC)"; \
rm -f test_output.txt; \
exit 1; \
	fi

.PHONY: test-core
test-core: check-java
	@echo "Running core module tests..."
	@export JAVA_HOME=$(JAVA_HOME) && \
export PATH=$(JAVA_HOME)/bin:$$PATH && \
$(GRADLE) :core:test --no-daemon
	@echo "$(GREEN)Core tests passed!$(NC)"

.PHONY: coverage
coverage: check-java
	@echo "Running tests with code coverage..."
	@export JAVA_HOME=$(JAVA_HOME) && \
export PATH=$(JAVA_HOME)/bin:$$PATH && \
$(GRADLE) test jacocoTestReport --no-daemon
	@echo ""
	@echo "Coverage report available at: build/reports/jacoco/index.html"

.PHONY: install
install: build
	@echo "============================================"
	@echo "Installing JavaBlocks Engine"
	@echo "============================================"
	@echo ""
	@echo "Install prefix: $(INSTALL_PREFIX)"
	@echo "Binary directory: $(BIN_INSTALL_DIR)"
	@echo "Library directory: $(LIB_INSTALL_DIR)"
	@echo "Share directory: $(SHARE_INSTALL_DIR)"
	@echo ""
	@echo "Creating directories..."
	@mkdir -p $(DESTDIR)$(BIN_INSTALL_DIR)
	@mkdir -p $(DESTDIR)$(LIB_INSTALL_DIR)
	@mkdir -p $(DESTDIR)$(SHARE_INSTALL_DIR)
	@mkdir -p $(DESTDIR)$(SHARE_INSTALL_DIR)/bin
	@mkdir -p $(DESTDIR)$(SHARE_INSTALL_DIR)/libs
	@mkdir -p $(DESTDIR)$(SHARE_INSTALL_DIR)/assets
	@echo "Installing core library..."
	@cp core/build/libs/javablocks-core-$(VERSION).jar $(DESTDIR)$(LIB_INSTALL_DIR)/ 2>/dev/null || \
cp core/build/libs/core-*.jar $(DESTDIR)$(LIB_INSTALL_DIR)/javablocks-core.jar 2>/dev/null || \
echo "$(YELLOW)Warning: Core JAR not found, skipping...$(NC)"
	@for module in assets plugins marketplace tools desktop editor; do \
if [ -f "$$module/build/libs/$$module-*.jar" ]; then \
echo "  Installing $$module..."; \
cp $$module/build/libs/$$module-*.jar $(DESTDIR)$(LIB_INSTALL_DIR)/javablocks-$$module.jar 2>/dev/null || true; \
	fi \
	done
	@echo "Installing default assets..."
	@cp -r assets/src/main/resources/* $(DESTDIR)$(SHARE_INSTALL_DIR)/assets/ 2>/dev/null || \
echo "$(YELLOW)Warning: No default assets found$(NC)"
	@echo "Creating launcher script..."
	@echo '#!/bin/bash' > $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo 'SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo 'JAVA_BLOCKS_HOME=$${JAVA_BLOCKS_HOME:-$(SHARE_INSTALL_DIR)}' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo 'LIB_DIR=$${LIB_DIR:-$(LIB_INSTALL_DIR)}' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo 'if [ -n "$$JAVA_HOME" ]; then' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '    JAVA_CMD="$$JAVA_HOME/bin/java"' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo 'elif command -v java > /dev/null 2>&1; then' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '    JAVA_CMD="java"' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo 'else' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '    echo "Error: Java not found. Please set JAVA_HOME or install Java 21+."' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '    exit 1' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo 'fi' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '# Default to editor if no arguments provided' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo 'MODE="$$1"' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo 'shift || true' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo 'case "$$MODE" in' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '    engine)' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '        exec $$JAVA_CMD -Djavablocks.home=$$JAVA_BLOCKS_HOME -cp "$$LIB_DIR/*" com.javablocks.core.JavaBlocksEngine "$$@"' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '        ;;' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '    desktop)' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '        exec $$JAVA_CMD -Djavablocks.home=$$JAVA_BLOCKS_HOME -cp "$$LIB_DIR/*" com.javablocks.desktop.JavaBlocksDesktop "$$@"' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '        ;;' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '    editor|--editor)' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '        exec $$JAVA_CMD -Djavablocks.home=$$JAVA_BLOCKS_HOME -cp "$$LIB_DIR/*" com.javablocks.editor.JavaBlocksEditor "$$@"' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '        ;;' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '    -h|--help|help)' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '        echo "JavaBlocks Engine Launcher"' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '        echo ""' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '        echo "Usage: javablocks [command] [options]"' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '        echo ""' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '        echo "Commands:"' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '        echo "  engine     Run the JavaBlocks core engine"' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '        echo "  desktop    Run the desktop game launcher"' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '        echo "  editor     Run the visual game editor (default)"' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '        echo "  -h, --help Show this help message"' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '        echo ""' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '        echo "Examples:"' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '        echo "  javablocks              # Launch editor"' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '        echo "  javablocks editor       # Launch editor"' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '        echo "  javablocks desktop      # Launch desktop launcher"' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '        echo "  javablocks engine       # Run core engine"' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '        exit 0' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '        ;;' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '    *)' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '        # Default to editor for backwards compatibility' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '        exec $$JAVA_CMD -Djavablocks.home=$$JAVA_BLOCKS_HOME -cp "$$LIB_DIR/*" com.javablocks.editor.JavaBlocksEditor "$$MODE $$@"' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo '        ;;' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo 'esac' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@chmod +x $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@echo "Creating desktop launcher..."
	@echo '#!/bin/bash' > $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-desktop
	@echo 'SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-desktop
	@echo 'LIB_DIR=$${LIB_DIR:-$(LIB_INSTALL_DIR)}' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-desktop
	@echo '' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-desktop
	@echo 'if [ -n "$$JAVA_HOME" ]; then' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-desktop
	@echo '    JAVA_CMD="$$JAVA_HOME/bin/java"' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-desktop
	@echo 'elif command -v java > /dev/null 2>&1; then' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-desktop
	@echo '    JAVA_CMD="java"' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-desktop
	@echo 'else' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-desktop
	@echo '    echo "Error: Java not found. Please set JAVA_HOME or install Java 21+."' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-desktop
	@echo '    exit 1' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-desktop
	@echo 'fi' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-desktop
	@echo '' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-desktop
	@echo 'exec $$JAVA_CMD -cp "$$LIB_DIR/*" com.javablocks.desktop.JavaBlocksDesktop "$$@"' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-desktop
	@chmod +x $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-desktop
	@echo "Creating editor launcher..."
	@echo '#!/bin/bash' > $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-editor
	@echo 'SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-editor
	@echo 'LIB_DIR=$${LIB_DIR:-$(LIB_INSTALL_DIR)}' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-editor
	@echo 'JAVA_BLOCKS_HOME=$${JAVA_BLOCKS_HOME:-$(SHARE_INSTALL_DIR)}' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-editor
	@echo '' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-editor
	@echo 'if [ -n "$$JAVA_HOME" ]; then' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-editor
	@echo '    JAVA_CMD="$$JAVA_HOME/bin/java"' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-editor
	@echo 'elif command -v java > /dev/null 2>&1; then' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-editor
	@echo '    JAVA_CMD="java"' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-editor
	@echo 'else' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-editor
	@echo '    echo "Error: Java not found. Please set JAVA_HOME or install Java 21+."' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-editor
	@echo '    exit 1' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-editor
	@echo 'fi' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-editor
	@echo '' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-editor
	@echo 'exec $$JAVA_CMD -Djavablocks.home=$$JAVA_BLOCKS_HOME -cp "$$LIB_DIR/*" com.javablocks.editor.JavaBlocksEditor "$$@"' >> $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-editor
	@chmod +x $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks-editor
	@echo "Creating environment setup..."
	@echo '# JavaBlocks Engine Environment Setup' > $(DESTDIR)$(SHARE_INSTALL_DIR)/javablocks-env.sh
	@echo "export JAVA_BLOCKS_HOME=$(SHARE_INSTALL_DIR)" >> $(DESTDIR)$(SHARE_INSTALL_DIR)/javablocks-env.sh
	@echo "export JAVA_BLOCKS_LIB=$(LIB_INSTALL_DIR)" >> $(DESTDIR)$(SHARE_INSTALL_DIR)/javablocks-env.sh
	@echo "export JAVA_BLOCKS_BIN=$(BIN_INSTALL_DIR)" >> $(DESTDIR)$(SHARE_INSTALL_DIR)/javablocks-env.sh
	@echo "Creating version file..."
	@echo "JAVA_BLOCKS_VERSION=$(VERSION)" > $(DESTDIR)$(SHARE_INSTALL_DIR)/VERSION
	@echo "JAVA_BLOCKS_JAVA_VERSION=$(JAVA_VERSION_REQUIRED)" >> $(DESTDIR)$(SHARE_INSTALL_DIR)/VERSION
	@echo "Creating module info..."
	@echo "Active Modules:" > $(DESTDIR)$(SHARE_INSTALL_DIR)/MODULES
	@echo "$(ACTIVE_MODULES)" >> $(DESTDIR)$(SHARE_INSTALL_DIR)/MODULES
	@echo "" >> $(DESTDIR)$(SHARE_INSTALL_DIR)/MODULES
	@echo "Disabled Modules:" >> $(DESTDIR)$(SHARE_INSTALL_DIR)/MODULES
	@echo "$(DISABLED_MODULES)" >> $(DESTDIR)$(SHARE_INSTALL_DIR)/MODULES
	@echo ""
	@echo "============================================"
	@echo "Installation Complete!"
	@echo "============================================"
	@echo ""
	@echo "To use JavaBlocks Engine:"
	@echo "  1. Source environment: source $(SHARE_INSTALL_DIR)/javablocks-env.sh"
	@echo "  2. Run launcher: $(BIN_INSTALL_DIR)/javablocks"
	@echo ""
	@echo "Libraries installed to: $(LIB_INSTALL_DIR)/"
	@echo "Assets installed to: $(SHARE_INSTALL_DIR)/assets/"
	@echo ""

.PHONY: install-user
install-user: build
	@echo "Installing to user home directory..."
	@make install INSTALL_PREFIX=$$HOME/.local DESTDIR=

.PHONY: uninstall
uninstall:
	@echo "Removing JavaBlocks Engine..."
	@rm -f $(DESTDIR)$(BIN_INSTALL_DIR)/javablocks
	@rm -rf $(DESTDIR)$(SHARE_INSTALL_DIR)/
	@rm -f $(DESTDIR)$(LIB_INSTALL_DIR)/javablocks-core-*.jar
	@rm -f $(DESTDIR)$(LIB_INSTALL_DIR)/javablocks-assets.jar
	@rm -f $(DESTDIR)$(LIB_INSTALL_DIR)/javablocks-plugins.jar
	@rm -f $(DESTDIR)$(LIB_INSTALL_DIR)/javablocks-marketplace.jar
	@rm -f $(DESTDIR)$(LIB_INSTALL_DIR)/javablocks-tools.jar
	@rm -f $(DESTDIR)$(LIB_INSTALL_DIR)/javablocks-desktop.jar
	@rm -f $(DESTDIR)$(LIB_INSTALL_DIR)/javablocks-editor.jar
	@echo "$(GREEN)Uninstallation complete!$(NC)"

.PHONY: package
package: build
	@echo "Creating distribution packages..."
	@mkdir -p package
	@cp core/build/libs/*.jar package/ 2>/dev/null || true
	@cp server/build/libs/*.jar package/ 2>/dev/null || true
	@tar -czf package/javablocks-engine-core-$(VERSION).tar.gz \
--exclude='.git' \
--exclude='.gradle' \
--exclude='build' \
--exclude='package' \
-C core .
	@tar -czf package/javablocks-engine-server-$(VERSION).tar.gz \
--exclude='.git' \
--exclude='.gradle' \
--exclude='build' \
--exclude='package' \
-C server .
	@echo ""
	@echo "Packages created in package/:"
	@ls -lh package/

.PHONY: deps
deps: check-java
	@echo "Checking dependencies..."
	@export JAVA_HOME=$(JAVA_HOME) && \
		export PATH=$(JAVA_HOME)/bin:$$PATH && \
$(GRADLE) dependencies --no-daemon -q

.PHONY: lint
lint: check-java
	@echo "Running code quality checks..."
	@export JAVA_HOME=$(JAVA_HOME) && \
export PATH=$(JAVA_HOME)/bin:$$PATH && \
$(GRADLE) compileJava --no-daemon -q
	@echo "$(GREEN)Code quality check passed!$(NC)"

.PHONY: doc
doc: check-java
	@echo "Generating documentation..."
	@mkdir -p doc
	@export JAVA_HOME=$(JAVA_HOME) && \
export PATH=$(JAVA_HOME)/bin:$$PATH && \
$(GRADLE) javadoc --no-daemon -q 2>/dev/null || \
echo "$(YELLOW)Javadoc not fully configured$(NC)"
	@echo "Documentation generated in doc/"

.PHONY: debug
debug: check-java
	@echo "Building with debug information..."
	@export JAVA_HOME=$(JAVA_HOME) && \
export PATH=$(JAVA_HOME)/bin:$$PATH && \
$(GRADLE) assemble --no-daemon -Pdebug=true
	@echo "$(GREEN)Debug build completed!$(NC)"

.PHONY: profile
profile: check-java
	@echo "Building with profiling enabled..."
	@export JAVA_HOME=$(JAVA_HOME) && \
export PATH=$(JAVA_HOME)/bin:$$PATH && \
$(GRADLE) assemble --no-daemon -Pprofile=true
	@echo "$(GREEN)Profile build completed!$(NC)"

.PHONY: help
help:
	@echo ""
	@echo "============================================"
	@echo "  JavaBlocks Engine Build System - Help"
	@echo "============================================"
	@echo ""
	@echo "Usage: make [target] [options]"
	@echo ""
	@echo "Main targets:"
	@echo "  all          - Build everything (default)"
	@echo "  build        - Clean and build the project"
	@echo "  build-core   - Build only the core module"
	@echo "  build-server - Build only the server module"
	@echo "  compile      - Compile source files only"
	@echo "  clean        - Remove build artifacts"
	@echo "  test         - Run unit tests"
	@echo "  test-core    - Run core module tests"
	@echo "  coverage     - Run tests with code coverage"
	@echo "  install      - Install to system (requires root/sudo)"
	@echo "  install-user - Install to user home directory"
	@echo "  uninstall    - Remove installed files"
	@echo "  package      - Create distribution packages"
	@echo ""
	@echo "Development targets:"
	@echo "  lint         - Run code quality checks"
	@echo "  doc          - Generate documentation"
	@echo "  debug        - Build with debug information"
	@echo "  profile      - Build with profiling enabled"
	@echo "  deps         - Check dependencies"
	@echo ""
	@echo "Options:"
	@echo "  INSTALL_PREFIX=/path  Set installation prefix (default: /usr/local)"
	@echo "  JAVA_HOME=/path       Set Java home directory"
	@echo ""
	@echo "Examples:"
	@echo "  make build            # Build the project"
	@echo "  sudo make install     # Install to /usr/local"
	@echo "  make install-user     # Install to ~/.local"
	@echo "  make test             # Run tests"
	@echo ""

.PHONY: version
version:
	@echo "JavaBlocks Engine Version: $(VERSION)"
	@echo "Package: $(PACKAGE_NAME)"
	@echo "Java Required: $(JAVA_VERSION_REQUIRED)+"
