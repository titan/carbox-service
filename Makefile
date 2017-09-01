NAME = box-service
BUILDDIR=/dev/shm/${NAME}

SERVERSRC:=$(BUILDDIR)/src/box_service.nim
BUILDSRC:=$(BUILDDIR)/box_service.nimble

all: $(SERVERSRC) $(BUILDSRC)

$(SERVERSRC): src/service.org | prebuild
	emacs $< --batch -f org-babel-tangle --kill

$(BUILDSRC): src/build.org | prebuild
	emacs $< --batch -f org-babel-tangle --kill

prebuild:
ifeq "$(wildcard $(BUILDDIR))" ""
	@mkdir -p $(BUILDDIR)/src
endif

clean:
	rm -rf $(BUILDDIR)

.PHONY: all clean prebuild
