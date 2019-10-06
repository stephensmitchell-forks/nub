/****************************************************************************************
 * nub
 * Copyright (c) 2019 National University of Colombia, https://visualcomputing.github.io/
 * @author Sebastian Chaparro, https://github.com/sechaparroc
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package nub.ik.solver.geometric;

import nub.core.Node;
import nub.ik.animation.*;
import nub.ik.solver.KinematicStructure;
import nub.ik.solver.Solver;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

//TODO : Enable / Disable iteration Hist

public class CCDSolver extends Solver {
  protected List<? extends Node> _chain;
  //TODO: This is just for debug purposes
  public boolean _enable_kinematic_structure = true;
  //Using a Kinematic Structure decreases the number of transformations
  protected List<KinematicStructure.KNode> _structure;
  protected Node _target;
  protected Node _previousTarget;

  //Animation Stuff
  //TODO: Refactor, perhaps move to Solver class
  //TODO: Refactor, perhaps move to Solver class
  protected int _last_time_event = 0;

  public List<? extends Node> chain() {
    return _chain;
  }

  public Node target() {
    return _target;
  }

  public void setTarget(Node endEffector, Node target) {
    this._target = target;
  }

  public void setTarget(Node target) {
    this._target = target;
  }

  public Node head() {
    return _chain.get(0);
  }

  public Node endEffector() {
    return _chain.get(_chain.size() - 1);
  }

  public CCDSolver(List<? extends Node> chain) {
    this(chain, null);
  }

  public CCDSolver(List<? extends Node> chain, boolean enable_kinematic_structure) {
        this(chain, null, enable_kinematic_structure);
    }

  public CCDSolver(List<? extends Node> chain, Node target) {
    this(chain, target, true);
  }

  public CCDSolver(List<? extends Node> chain, Node target, boolean enable_kinematic_structure) {
    super();
    _enable_kinematic_structure = enable_kinematic_structure;
    if(_enable_kinematic_structure) this._structure = KinematicStructure.generateKChain(chain);
    else this._chain = chain;
    this._target = target;
    this._previousTarget =
        target == null ? null : new Node(target.position().get(), target.orientation().get(), 1);
  }

  /*
   * Performs a CCD ITERATION
   * For further info please look at (https://sites.google.com/site/auraliusproject/ccd-algorithm)
   * */
  @Override
  protected boolean _iterate() {
    if(_enable_kinematic_structure) return iterateWithStructure();
    return iterateWithChain();
  }

  //TODO: this must be removed
  protected boolean iterateWithChain(){
    //As no target is specified there is no need to perform an iteration
    if (_target == null || _chain.size() < 2) return true;
    Node end = _chain.get(_chain.size() - 1);
    Vector target = this._target.position().get();
    //Execute Until the distance between the end effector and the target is below a threshold
    if (Vector.distance(end.position(), target) <= _maxError) {
      return true;
    }
    float change = 0.0f;
    Vector endLocalPosition = _chain.get(_chain.size() - 2).location(end.position());
    Vector targetLocalPosition = _chain.get(_chain.size() - 2).location(target);

    for (int i = _chain.size() - 2; i >= 0; i--) {
      Quaternion initial = _chain.get(i).rotation().get();
      Quaternion delta = new Quaternion(endLocalPosition, targetLocalPosition);
      //update target local position
      if (_chain.get(i).reference() == null) {
        targetLocalPosition = _chain.get(i).worldLocation(targetLocalPosition);
      } else {
        targetLocalPosition = _chain.get(i).reference().location(targetLocalPosition, _chain.get(i));
      }
      _chain.get(i).rotate(delta);
      //update end effector local position
      if (_chain.get(i).reference() == null) {
        endLocalPosition = _chain.get(i).worldLocation(endLocalPosition);
      } else {
        endLocalPosition = _chain.get(i).reference().location(endLocalPosition, _chain.get(i));
      }
      initial.compose(_chain.get(i).rotation().inverse());
      change += Math.abs(initial.angle());
    }
    //Check total rotation change
    if (change <= _minDistance) return true;
    return false;
  }

  protected boolean iterateWithStructure(){
    //As no target is specified there is no need to perform an iteration
    if (_target == null || _structure.size() < 2) return true;
    KinematicStructure.KNode end = _structure.get(_structure.size() - 1);
    Vector target = this._target.position().get();
    //update whole chain position/orientation
    Vector endPosition = end.position().get();
    //Execute Until the distance between the end effector and the target is below a threshold
    if (Vector.distance(end.position(), target) <= _maxError) {
      return true;
    }

    float change = 0.0f;

    //Traverse the structure backwards
    for (int i = _structure.size() - 2; i >= 0; i--) {
      Vector targetLocalPosition = _structure.get(i).location(target, true);
      Vector endLocalPosition = _structure.get(i).location(end.position(), true);
      if(_enableMediator){
        //STEP 1:
        //Create the event
        InterestingEvent event1a = new InterestingEvent("E1A", "Trajectory", _last_time_event, 1, 2);
        //Add the convenient attributes
        event1a.addAttribute("positions", _structure.get(i).position(), end.position());
        //Add it to the event queue TODO: Replace the event queue by a mediator class
        mediator().addEvent(event1a);

        InterestingEvent event1b = new InterestingEvent("E1B", "Trajectory", _last_time_event, 1, 3);
        //Add the convenient attributes
        event1b.addAttribute("reference", _structure.get(i).node());
        event1b.addAttribute("positions", new Vector(), endLocalPosition);
        //Add it to the event queue TODO: Replace the event queue by a mediator class
        mediator().addEvent(event1b);

        //Create the event
        InterestingEvent event2 = new InterestingEvent("E2", "Message", _last_time_event, 0, 1);
        //Add the convenient attributes
        event2.addAttribute("message", "Step 1: Find the segment line defined by Joint " + i + " and End Effector " + (_structure.size() -1));
        //Add it to the event queue
        mediator().addEvent(event2);

        //STEP 2
        //Create the event
        InterestingEvent event3 = new InterestingEvent("E3", "Trajectory", _last_time_event + 1, 1, 2);
        //Add the convenient attributes
        event3.addAttribute("positions", _structure.get(i).position(), target);
        //Add it to the event queue
        mediator().addEvent(event3);
        //Create the event
        InterestingEvent event4 = new InterestingEvent("E4", "Message", _last_time_event + 1, 0, 1);
        //Add the convenient attributes
        event4.addAttribute("message", "Step 2: Find the segment Line defined by Joint " + i + " and Target");
        //Add it to the event queue
        mediator().addEvent(event4);
      }

      Quaternion delta = new Quaternion(endLocalPosition, targetLocalPosition);
      _structure.get(i).rotate(delta);


      if(_enableMediator){
        //STEP 3:
        //Create the event
        InterestingEvent event5 = new InterestingEvent("E5", "NodeRotation", _last_time_event + 2, 1, 1);
        //Add the convenient attributes
        event5.addAttribute("node", _structure.get(i).node());
        event5.addAttribute("rotation", delta);
        //Add it to the event queue TODO: Replace the event queue by a mediator class
        mediator().addEvent(event5);
        //Create the event
        InterestingEvent event6 = new InterestingEvent("E6", "Message", _last_time_event + 2, 0, 1);
        //Add the convenient attributes
        event6.addAttribute("message", "Step 3: Rotate Joint " + i + " to reduce the distance from End Effector " + i + " to Target (T)");
        //Add it to the event queue
        mediator().addEvent(event6);
        _last_time_event += 3;
      }
    }
    //Check total change
    if (Vector.distance(endPosition, end.position()) <= _minDistance) return true;
    return false;
  }


  @Override
  protected void _update() {
    /*Not required, since chain is updated inside iterate step*/
  }

  @Override
  protected boolean _changed() {
    if (_target == null) {
      _previousTarget = null;
      return false;
    } else if (_previousTarget == null) {
      return true;
    }
    return !(_previousTarget.position().matches(_target.position()) && _previousTarget.orientation().matches(_target.orientation()));
  }

  @Override
  protected void _reset() {
    _previousTarget = _target == null ? null : new Node(_target.position().get(), _target.orientation().get(), 1);
    _iterations = 0;
    if(_enableMediator){
      InterestingEvent event = new InterestingEvent("R", "UpdateStructure", _last_time_event, 1, 1);
      if(_enable_kinematic_structure){
        List<Node> chain = new ArrayList<Node>();
        int i = 0;
        Vector[] translations = new Vector[_structure.size()];
        Quaternion[] rotations = new Quaternion[_structure.size()];
        for(KinematicStructure.KNode knode : _structure){
          chain.add(knode.node());
          translations[i] = knode.node().translation().get();
          rotations[i++] = knode.node().rotation().get();
        }
        event.addAttribute("structure", chain);
        event.addAttribute("rotations", rotations);
        event.addAttribute("translations", translations);
      }else{
        int i = 0;
        Vector[] translations = new Vector[_chain.size()];
        Quaternion[] rotations = new Quaternion[_chain.size()];
        for(Node node : _chain){
          translations[i] = node.translation().get();
          rotations[i++] = node.rotation().get();
        }
        event.addAttribute("structure", _chain);
        event.addAttribute("rotations", rotations);
        event.addAttribute("translations", translations);
      }
      mediator().addEvent(event);
      InterestingEvent messageEvent = new InterestingEvent("RM", "Message", _last_time_event++, 0, 1);
      //Add the convenient attributes
      messageEvent.addAttribute("message", "Updating structure");
      mediator().addEvent(messageEvent);
    }
    if(_enable_kinematic_structure){
        //TODO: Remove this update!
        _structure.get(_structure.size()-1).updatePath(null);
    }
  }

  @Override
  public float error() {
    if(_enable_kinematic_structure) return Vector.distance(_structure.get(_structure.size() - 1).position(), _target.position());
    return Vector.distance(_chain.get(_chain.size() - 1).position(), _target.position());
  }

  //Animation Stuff
  //TODO: Refactor, perhaps move to Solver class
  @Override
  public void registerStructure(VisualizerMediator mediator){
      if(_enable_kinematic_structure){
          List<Node> chain = new ArrayList<Node>();
          for(KinematicStructure.KNode knode : _structure){
              chain.add(knode.node());
          }
          mediator.registerStructure(chain);
      }else {
          mediator.registerStructure(_chain);
      }
  }

  @Override
  public Iterator<? extends Node> iterator(){
    if(_enable_kinematic_structure){
      ArrayList<Node> chain = new ArrayList<Node>();
      for(KinematicStructure.KNode knode : _structure){
        chain.add(knode.node());
      }
      return chain.iterator();
    }
    return _chain.iterator();
  }
}
