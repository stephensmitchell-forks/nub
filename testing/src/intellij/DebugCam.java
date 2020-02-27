package intellij;

import nub.core.Node;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;

public class DebugCam extends PApplet {
  Scene scene;
  Vector axis;
  boolean cad, peasy;
  float inertia = 0.8f;

  public void settings() {
    size(800, 800, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    //scene.togglePerspective();
    Node box1 = new Node(scene) {
      @Override
      public void graphics(PGraphics pg) {
        pg.pushStyle();
        pg.strokeWeight(1 / 10f);
        pg.fill(255, 0, 0);
        pg.box(30);
        pg.popStyle();
      }
      @Override
      public void interact(Object... gesture) {
        if (gesture.length == 1) {
          if (gesture[0] instanceof Float) {
            /*
            // Orbit
            //orbit(axis, (float)gesture[0], new Vector(0,0,0));
            //orbit(new Quaternion(displacement(axis), (float)gesture[0]), new Vector(0,0,0));
            Quaternion q = new Quaternion(displacement(axis), (float) gesture[0]);
            orbit(q, scene.center(), 0.2f);
            Vector e =  q.eulerAngles();
            //orbit(new Quaternion(displacement(axis), (float)gesture[0]), new Vector(0,0,0));
            _orbitTask._x += e.x();
            _orbitTask._y += e.y();
            _orbitTask._z += e.z();
            if (!_orbitTask.isActive())
              _orbitTask.run();
            // */
            // /*
            // Scaling
            float factor = 1 + (Math.abs((float) gesture[0]) / graph().height());
            //scale(delta >= 0 ? factor : 1 / factor);
            scale((float) gesture[0] >= 0 ? factor : 1 / factor, 0.2f);

            //float dx = Math.abs((float)gesture[0]);
            //float factor = 1 + dx / graph().height();
            //scale(dx >= 0 ? factor : 1 / factor);
            //scale(dx >= 0 ? factor : 1 / factor, 0.2f);
            //_scalingTask._x += (float)gesture[0];
            //if (!_scalingTask.isActive())
            //  _scalingTask.run();
            // */
            /*
            // Rotation
            Quaternion q = new Quaternion(displacement(axis), (float) gesture[0]);
            Vector e = q.eulerAngles();
            //orbit(new Quaternion(displacement(axis), (float)gesture[0]), new Vector(0,0,0));
            _rotationTask._x += (float) gesture[0];
            //_rotationTask._y += e.y();
            //_rotationTask._z += e.z();
            if (!_rotationTask.isActive())
              _rotationTask.run();
            // */
            /*
            Quaternion q = new Quaternion((float) gesture[0], 0, 0);
            rotate(q, 0f);
            // */
          }
        }
      }
    };
    box1.setPickingThreshold(0);
    Node box2 = new Node(box1) {
      @Override
      public void graphics(PGraphics pg) {
        pg.pushStyle();
        pg.strokeWeight(1 / 10f);
        pg.fill(0, 0, 255);
        pg.box(5);
        pg.popStyle();
      }
    };
    box2.setPickingThreshold(0);
    box2.translate(0, 0, 20);
    scene.setRadius(50);
    scene.fit(1);
    axis = Vector.random();
    axis.multiply(scene.radius() / 3);
  }

  public void draw() {
    background(0);
    scene.drawAxes();
    scene.drawArrow(axis);
    stroke(125);
    scene.drawGrid();
    lights();
    scene.render();
  }

  public void mouseMoved() {
    scene.mouseTag();
  }

  public void mouseDragged() {
    switch (mouseButton) {
      case LEFT:
        if (!scene.mouseSpinTag(inertia))
          if (peasy)
            scene.mouseDebugSpinEye(inertia);
          else
            scene.mouseSpinEye(inertia);
        //scene.mouseSpin();
        break;
      case RIGHT:
        if (!scene.mouseTranslateTag(inertia))
          scene.mouseTranslateEye(inertia);
        break;
      case CENTER:
        if (!scene.interactTag((float) mouseX - pmouseX))
          if (cad)
            scene.mouseRotateCAD(inertia);
          else
            scene.mouseLookAround(inertia);
        break;
    }
  }

  public void mouseWheel(MouseEvent event) {
    if (!scene.interactTag((float)event.getCount() * 10.f * PI / (float)width))
      scene.moveForward(event.getCount() * 20, inertia);
  }

  public void keyPressed() {
    if (key == 'f')
      scene.flip();
    if (key == 'p')
      peasy = !peasy;
    if (key == 'c')
      cad = !cad;
    if (key == 's')
      scene.fit(1);
    if (key == 'S')
      scene.fit();
    if (key == '0')
      inertia = 0.f;
    if (key == '1')
      inertia = 0.1f;
    if (key == '2')
      inertia = 0.2f;
    if (key == '3')
      inertia = 0.3f;
    if (key == '4')
      inertia = 0.4f;
    if (key == '5')
      inertia = 0.5f;
    if (key == '6')
      inertia = 0.6f;
    if (key == '7')
      inertia = 0.7f;
    if (key == '8')
      inertia = 0.8f;
    if (key == '9')
      inertia = 0.9f;
    if (key == 'd')
      inertia = 1.f;
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.DebugCam"});
  }
}
