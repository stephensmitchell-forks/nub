package intellij;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;
import processing.opengl.PShader;

import java.nio.file.Paths;

public class ShadowMap extends PApplet {
  Scene scene;
  Scene shadowMapScene;
  Node[] shapes;
  PGraphics shadowMap;
  PShader depthShader;
  float zNear = 50;
  float zFar = 700;
  int w = 700;
  int h = 700;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    shadowMap = createGraphics(w / 2, h / 2, P3D);
    depthShader = loadShader(Paths.get("testing/data/depth/depth_linear.glsl").toAbsolutePath().toString());
    depthShader.set("near", zNear);
    depthShader.set("far", zFar);
    shadowMap.shader(depthShader);
    scene = new Scene(this);
    scene.enableHint(Scene.CENTER);
    scene.enableHint(Scene.BACKGROUND, color(75, 25, 15));
    scene.setRadius(max(w, h));
    scene.fit(1);
    shapes = new Node[20];
    for (int i = 0; i < shapes.length; i++) {
      shapes[i] = new Node(this::cube);
      scene.randomize(shapes[i]);
      shapes[i].setHighlight(0);
    }
    scene.tag("light", shapes[(int) random(0, shapes.length - 1)]);
    scene.node("light").toggleHint(Node.SHAPE | Node.AXES | Node.FRUSTUM);
    scene.node("light").setOrientation(Quaternion.from(Vector.plusK, scene.node("light").position()));
    // scene.enablePicking(false);
    shadowMapScene = new Scene(shadowMap, scene.node("light"), zNear, zFar);
    shadowMapScene.togglePerspective();
    shadowMapScene.enableHint(Scene.AXES | Scene.CENTER);
    shadowMapScene.enableHint(Scene.BACKGROUND, color(140, 160, 125));
    shadowMapScene.picking = false;
    frameRate(1000);
  }

  public void cube(PGraphics pg) {
    pg.pushStyle();
    if (pg == shadowMap)
      pg.noStroke();
    else {
      pg.strokeWeight(3);
      pg.stroke(0, 255, 255);
    }
    pg.fill(255, 0, 0);
    pg.box(80);
    pg.popStyle();
  }

  public void draw() {
    // 1. Fill in and display front-buffer
    scene.render();
    // 2. Fill in shadow map using the light point of view
    if (scene.isTagValid("light")) {
      shadowMapScene.display(w / 2, h / 2);
    }
  }

  public void mouseMoved(MouseEvent event) {
    if (event.isShiftDown()) {
      if (scene.isTagValid("light")) {
        scene.node("light").toggleHint(Node.SHAPE | Node.AXES | Node.FRUSTUM);
      }
      // no calling mouseTag since we need to immediately update the tagged node
      scene.updateMouseTag("light");
      if (scene.isTagValid("light")) {
        shadowMapScene.setEye(scene.node("light"));
        scene.node("light").toggleHint(Node.SHAPE | Node.AXES | Node.FRUSTUM);
      }
    } else
      scene.mouseTag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.mouseSpin();
    else if (mouseButton == RIGHT)
      scene.mouseTranslate();
    else
      scene.moveForward(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    if (event.isShiftDown() && scene.isTagValid("light")) {
      depthShader.set("far", zFar += event.getCount() * 20);
      shadowMapScene.setFrustum(zNear, zFar);
    }
    else
      scene.scale(event.getCount() * 20);
  }

  public void keyPressed() {
    if (key == ' ' && scene.isTagValid("light")) {
      shadowMapScene.togglePerspective();
    }
    if (key == 'p')
      scene.togglePerspective();
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.ShadowMap"});
  }
}
