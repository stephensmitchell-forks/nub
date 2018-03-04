framesjs
[![All Contributors](https://img.shields.io/badge/all_contributors-0-orange.svg?style=flat-square)](#contributors)

**Table of Contents**

- [Description](#user-content-description)
- [Usage](#user-content-usage)
- [Key-frame interpolators](#user-content-interpolators)
- [Non-standard interactivity](#user-content-interactivity)
- [Drawing functionality](#user-content-drawing)
- [Installation](#user-content-installation)
- [Contributors](#user-content-contributors)

# Description

A 2D or 3D [Processing](http://forum.processing.org) scene-graph providing eye, input and timing handling to a raster or ray-tracing renderer.

# Usage

Typical usage comprises three steps: scene instantiation, setting an eye and setting some shapes.

## Scene instantiation

Instantiate your on-screen scene at the [setup()](https://processing.org/reference/setup_.html):

```java
Scene scene;
void setup() {
  scene = new Scene(this);
}
```

The `scene` [frontBuffer()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#frontBuffer--) corresponds to the *PApplet* main canvas.
 
Off-screen scenes should be instantiated upon a [PGraphics](https://processing.org/reference/PGraphics.html) object:

```java
Scene scene;
PGraphics canvas;
void setup() {
  canvas = createGraphics(500, 500, P3D);
  scene = new Scene(this, canvas);
}
```

In this case, the `scene` [frontBuffer()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#frontBuffer--) corresponds to the `canvas`.

## The eye

The scene eye can be an instance of [Frame](https://visualcomputing.github.io/frames-javadocs/frames/primitives/Frame.html) or a [Node](https://visualcomputing.github.io/frames-javadocs/frames/core/Node.html). To set the eye from a `frame` instance use code such as the following:

```java
...
Frame eye;
void setup() {
  ...
  eye = new Frame();
  scene.setEye(eye);
}
```

The eye can be controlled programmatically using the powerful [Frame](https://visualcomputing.github.io/frames-javadocs/frames/primitives/Frame.html) API.

To set the eye from a node instance use code such as the following:

```java
...
Node eye;
void setup() {
  ...
  eye = new Node(scene) {
    public void interact(Event event) {
      if (event.shortcut().matches(new Shortcut(PApplet.LEFT)))
        translate(event);
    }
  };
  scene.setEye(eye);
  scene.setDefaultGrabber(eye);
}
```

The eye can be controlled both programmatically (since a [Node](https://visualcomputing.github.io/frames-javadocs/frames/core/Node.html) is a [Frame](https://visualcomputing.github.io/frames-javadocs/frames/primitives/Frame.html) specialization)) and interactively (using the mouse, see [mouse()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#mouse--) and [Mouse](https://visualcomputing.github.io/frames-javadocs/frames/processing/Mouse.html)). Note the use of the anonymous inner [Node](https://visualcomputing.github.io/frames-javadocs/frames/core/Node.html) class used to define how the node will behave, refer to the [Node](https://visualcomputing.github.io/frames-javadocs/frames/core/Node.html) API for details. Note also the [setDefaultGrabber(Grabber)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#setDefaultGrabber-frames.input.Grabber-) call which will direct input to the eye when no other node is being picked.

## Shapes

A [Shape](https://visualcomputing.github.io/frames-javadocs/frames/processing/Shape.html) is a [Node](https://visualcomputing.github.io/frames-javadocs/frames/core/Node.html) specialization that can be set from a retained-mode rendering Processing [PShape](https://processing.org/reference/PShape.html) or from an immediate-mode rendering Processing procedure. Shapes can be picked precisely using their projection onto the screen, see [setPrecision(Node.Precision)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Shape.html#setPrecision-frames.core.Node.Precision-). Use [traverse()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#traverse--) to render all scene-graph shapes or [draw()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Shape.html#draw--) to render a specific one instead.

### Retained-mode shapes

To set a retained-mode shape use `Shape shape = new Shape(Scene scene, PShape shape)` or `Shape shape = new Shape(Scene scene)` and then call `Shape.set(PGraphics)`.

### Immediate-mode shapes

To set an immediate-mode shape use code such as the following:
 
```java
...
Shape shape;
void setup() {
  ...
  shape = new Shape(scene) {
    public void set(PGraphics canvas) {
      //immediate-mode rendering procedure
    }
  };
}

Note tha shapes like nodes can be control interactively. You may also override [interact(Event)](https://visualcomputing.github.io/frames-javadocs/frames/core/Node.html#interact-frames.input.Event-), like it has been done above.

# Key-frame interpolators

A frame (and hence a node or a shape) can be animated through a key-frame [Catmull-Rom](https://en.wikipedia.org/wiki/Cubic_Hermite_spline#Catmull%E2%80%93Rom_spline) interpolator path. Use code such as the following:

```java
Scene scene;
PShape pshape;
Shape shape;
Interpolator interpolator;
void setup() {
  ...
  shape = new Shape(scene, pshape);
  interpolator = new Interpolator(shape);
  for (int i = 0; i < random(4, 10); i++)
    interpolator.addKeyFrame(Node.random(scene));
  interpolator.start();
}

which will create a random interpolator path containing [4..10] key-frames. The interpolation is also started. The interpolator path may be drawn with code like this:

```java
...
void draw() {
  scene.traverse();
  scene.drawPath(interpolator, 5);
}

while `traverse()` will draw the animated shape(s) `drawPath(Interpolator, int)` will draw the interpolated path too.
 
# Non-standard interactivity
 
To control your scene nodes by means different than the [mouse()](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html#mouse--) (see [Mouse](https://visualcomputing.github.io/frames-javadocs/frames/processing/Mouse.html)), implement an [Agent](https://visualcomputing.github.io/frames-javadocs/frames/input/Agent.html) and call [registerAgent(Agent)](https://visualcomputing.github.io/frames-javadocs/frames/core/Graph.html#registerAgent-frames.input.Agent-).

# Drawing functionality

The [Scene](https://visualcomputing.github.io/frames-javadocs/frames/processing/Scene.html) implements several static drawing functions that complements those already provided by Processing, such as: `drawCylinder(PGraphics, int, float, float)}`, `drawHollowCylinder(PGraphics, int, float, float, Vector, Vector)`, `drawCone(PGraphics, int, float, float, float, float)`, `drawCone(PGraphics, int, float, float, float, float, float)` and `drawTorusSolenoid(PGraphics, int, int, float, float)`.

Drawing functions that take a `PGraphics` parameter (including the above static ones), such as `beginScreenCoordinates(PGraphics)`,
`endScreenCoordinates(PGraphics)`, `drawAxes(PGraphics, float)`, `drawCross(PGraphics, float, float, float)` and `drawGrid(PGraphics)` among others, can be used to set a `Shape` (see [set(PGraphics)](https://visualcomputing.github.io/frames-javadocs/frames/processing/Shape.html#set-processing.core.PShape-)).

Another scene's eye (different than this one) can be drawn with `drawEye(Graph)`. Typical usage include interactive [minimaps](https://en.wikipedia.org/wiki/Mini-map) and _visibility culling_ visualization and debugging.

# Installation

Import/update it directly from your PDE. Otherwise download your release from [here](https://github.com/VisualComputing/framesjs/releases) and extract it to your sketchbook `libraries` folder.


## Contributors

Thanks goes to these wonderful people ([emoji key](https://github.com/kentcdodds/all-contributors#emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore -->
<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/kentcdodds/all-contributors) specification. Contributions of any kind welcome!