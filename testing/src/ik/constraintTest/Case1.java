package ik.constraintTest;

import ik.basic.Util;
import nub.core.Graph;
import nub.core.Node;
import nub.core.constraint.BallAndSocket;
import nub.core.constraint.Hinge;
import nub.ik.solver.Solver;
import nub.ik.solver.evolutionary.BioIk;
import nub.ik.solver.geometric.CCDSolver;
import nub.ik.solver.geometric.ChainSolver;
import nub.ik.solver.geometric.oldtrik.TRIK;
import nub.ik.solver.trik.implementations.SimpleTRIK;
import nub.ik.visual.Joint;
import nub.primitives.Vector;
import nub.processing.Scene;
import nub.processing.TimingTask;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;

public class Case1 extends PApplet {
  Scene scene, auxiliar, focus;
  boolean displayAuxiliar = false;

  int numJoints = 3;
  float targetRadius = 7;
  float boneLength = 50;

  int rows = 2;

  //Benchmark Parameters
  ArrayList<Solver> solvers; //Will store Solvers
  ArrayList<ArrayList<Node>> structures = new ArrayList<>(); //Keep Structures
  ArrayList<Node> targets = new ArrayList<Node>(); //Keep targets

  int numSolvers = 8; //Set number of solvers
  boolean solve = false;

  public void settings() {
    size(700, 700, P3D);
  }

  public void setup() {
    TRIK._singleStep = true;

    scene = new Scene(this);
    scene.setType(Graph.Type.ORTHOGRAPHIC);
    scene.setRadius(numJoints * boneLength * 2.5f);
    scene.fit(1);
    scene.setRightHanded();

    auxiliar = new Scene(this, P3D, width, height, 0, 0);
    auxiliar.setType(Graph.Type.ORTHOGRAPHIC);
    auxiliar.setRadius(numJoints * boneLength * 2.5f);
    auxiliar.setRightHanded();
    auxiliar.fit();

    PShape redBall = createShape(SPHERE, targetRadius);
    redBall.setStroke(false);
    redBall.setFill(color(255, 0, 0));

    //create targets
    for (int i = 0; i < numSolvers; i++) {
      Node target = new Node(redBall);
      target.setPickingThreshold(0);
      targets.add(target);
    }

    //create skeleton
    int solversPerRow = (int) Math.ceil(1.f * numSolvers / rows);

    for (int i = 0; i < numSolvers; i++) {
      int row = i / solversPerRow;
      int col = i % solversPerRow;
      int cols = row == rows - 1 ? solversPerRow - (rows * solversPerRow - numSolvers) : solversPerRow;
      float xOffset = ((1.f * col) / (cols - 1)) * scene.radius() - scene.radius() / 2;
      float yOffset = -((1.f * row) / (rows - 1)) * scene.radius() + scene.radius() / 2;
      int red = (int) random(255), green = (int) random(255), blue = (int) random(255);

      structures.add(generateSkeleton(new Vector(xOffset, yOffset, 0), red, green, blue));
    }

    //create solvers
    solvers = new ArrayList<>();

    int i = 0;
    //CCD
    CCDSolver ccdSolver = new CCDSolver(structures.get(i++));
    solvers.add(ccdSolver);
    //BioIK
    solvers.add(new BioIk(structures.get(i++), 10, 4));
    //CCD TRIK
    SimpleTRIK simpleTRIK = new SimpleTRIK(structures.get(i++), SimpleTRIK.HeuristicMode.FINAL);
    solvers.add(simpleTRIK);

    //ChainSolver chainSolver;
    //chainSolver = new ChainSolver(structures.get(i++));
    //chainSolver.setKeepDirection(false);
    //chainSolver.setFixTwisting(false);

    //solvers.add(chainSolver);
    //FABRIK Keeping directions (H1)
    ChainSolver chainSolver = new ChainSolver(structures.get(i++));
    chainSolver.setFixTwisting(true);
    chainSolver.setKeepDirection(false);
    solvers.add(chainSolver);
    //FABRIK Fix Twisting (H2)
    chainSolver = new ChainSolver(structures.get(i++));
    chainSolver.setFixTwisting(false);
    chainSolver.setKeepDirection(true);
    solvers.add(chainSolver);
    //FABRIK Fix Twisting (H1 & H2)
    chainSolver = new ChainSolver(structures.get(i++));
    chainSolver.setFixTwisting(true);
    chainSolver.setKeepDirection(true);
    solvers.add(chainSolver);
    //HGSA
    BioIk bioIk = new BioIk(structures.get(i++), 20, 12);
    solvers.add(bioIk);
    //HGSA
    TRIK trik = new TRIK(structures.get(i++));
    trik.setLookAhead(2);
    trik.enableWeight(true);
    solvers.add(trik);

    for (i = 0; i < solvers.size(); i++) {
      Solver solver = solvers.get(i);
      //6. Define solver parameters
      solver.setMaxError(0.001f);
      solver.setTimesPerFrame(1);
      solver.setMaxIterations(200);
      //7. Set targets
      solver.setTarget(structures.get(i).get(numJoints - 1), targets.get(i));
      targets.get(i).setPosition(structures.get(i).get(numJoints - 1).position());

      TimingTask task = new TimingTask() {
        @Override
        public void execute() {
          if (solve) {
            solver.solve();
          }
        }
      };
      task.run(40);
    }

    //Define Text Font
    textFont(createFont("Zapfino", 38));
  }

  public void draw() {
    focus = displayAuxiliar ? auxiliar : scene;
    background(0);
    lights();
    scene.drawAxes();
    scene.render();
    scene.beginHUD();
    for (int i = 0; i < solvers.size(); i++) {
      Util.printInfo(scene, solvers.get(i), structures.get(i).get(0).position());
    }

    if (displayAuxiliar) {
      auxiliar.beginDraw();
      auxiliar.context().lights();
      auxiliar.context().background(0);
      auxiliar.drawAxes();
      auxiliar.render();
      auxiliar.endDraw();
      auxiliar.display();
    }
    scene.endHUD();

  }

  public ArrayList<Node> generateSkeleton(Vector position, int red, int green, int blue) {
    //3-Segment-Arm
    ArrayList<Node> skeleton = new ArrayList<>();
    Joint j1 = new Joint(red, green, blue);
    Joint j2 = new Joint(red, green, blue);
    j2.setReference(j1);
    j2.translate(boneLength, 0, 0);
    Joint j3 = new Joint();
    j3.setReference(j2);
    Vector v = new Vector(boneLength, -boneLength, 0);
    v.normalize();
    v.multiply(boneLength);
    j3.translate(v);
    j1.setRoot(true);
    j1.translate(position);
    skeleton.add(j1);
    skeleton.add(j2);
    skeleton.add(j3);
    //Add constraints
    BallAndSocket c1 = new BallAndSocket(radians(45), radians(45));
    c1.setRestRotation(j1.rotation().get(), new Vector(0, 1, 0), new Vector(1, 0, 0));
    c1.setTwistLimits(radians(0), radians(180));
    j1.setConstraint(c1);

    Hinge c2 = new Hinge(radians(0), radians(120), j2.rotation().get(), j3.translation().get(), new Vector(0, 0, -1));
    j2.setConstraint(c2);
    return skeleton;
  }


  @Override
  public void mouseMoved() {
    focus.mouseTag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT) {
      focus.mouseSpin();
    } else if (mouseButton == RIGHT) {
      if (targets.contains(focus.node())) {
        for (Node t : targets) focus.translateNode(t, focus.mouseDX(), focus.mouseDY(), 0, 0);
      } else {
        focus.mouseTranslate();
      }
    } else {
      focus.scale(mouseX - pmouseX);
    }
  }

  public void mouseWheel(MouseEvent event) {
    focus.scale(event.getCount() * 20);
  }

  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        focus.focus();
      else
        focus.align();
  }

  public void keyPressed() {
    if (key == 'w' || key == 'W') {
      solve = !solve;
    } else if (key == '1') {
      displayAuxiliar = true;
      for (Solver s : solvers) s.solve();
    } else if (key == '2') {
      displayAuxiliar = true;
      for (Solver s : solvers) s.solve();
    } else if (key == ' ') {
      displayAuxiliar = !displayAuxiliar;
    } else if (key == 's') {
      for (Solver s : solvers) s.solve();
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"ik.constraintTest.Case1"});
  }
}