APP_OPTIONS = pics/balls2.nff

SRC  = Cone3D.java
SRC += Light3D.java
SRC += Object3D.java
SRC += Polygon3D.java
SRC += Raytracer.java
SRC += Surface.java
SRC += TrianglePatch3D.java
SRC += VirtualScreen.java
SRC += World.java
SRC += GfxColor.java
SRC += WorldInterface.java
SRC += Plane3D.java
SRC += Ray3D.java
SRC += Sphere3D.java
SRC += Triangle3D.java
SRC += Vector3D.java
SRC += WhiteSurface.java

MAIN = Raytracer.java
OUT= raytracer

include ../../../config.mk
include ../config.mk

java: $(CLASS_FILES) $(MAIN_CLASS)

satin: java
	$(SATINC_COMMAND) -no-aborts $(MAIN_CLASS_NAME)
	$(SATINC_COMMAND) -no-aborts World $(MAIN_CLASS_NAME)

manta: $(MAIN) $(O_FILES)
	$(MANTAC_COMMAND) -o $(OUT) $(MAIN) $(O_FILES) ../../../../ibis/$(IBIS_LIB)

clean:
	rm -f $(TRASH_FILES)

include ../run.mk
