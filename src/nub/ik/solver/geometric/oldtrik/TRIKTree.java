package nub.ik.solver.geometric.oldtrik;

import nub.core.Node;
import nub.ik.solver.Solver;
import nub.ik.solver.trik.implementations.SimpleTRIK;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TRIKTree extends Solver {
    protected static class TreeNode {
        protected TreeNode _parent;
        protected List<TreeNode> _children;

        protected SimpleTRIK _solver;
        protected boolean _modified;
        protected float _weight = 1.f;

        public TreeNode() {
            _children = new ArrayList<TreeNode>();
        }

        public TreeNode(SimpleTRIK solver) {
            this._solver = solver;
            _solver.setTimesPerFrame(5);

            _children = new ArrayList<TreeNode>();
        }

        protected TreeNode(TreeNode parent, SimpleTRIK solver) {
            this._parent = parent;
            this._solver = solver;
            if (parent != null) {
                parent._addChild(this);
            }
            _children = new ArrayList<TreeNode>();
        }

        protected boolean _addChild(TreeNode n) {
            return _children.add(n);
        }

        protected List<TreeNode> _children() {
            return _children;
        }

        protected float _weight() {
            return _weight;
        }

        protected SimpleTRIK _solver() {
            return _solver;
        }
    }

    protected TreeNode _root;
    protected SimpleTRIK.HeuristicMode _mode = SimpleTRIK.HeuristicMode.EXPRESSIVE_FINAL;
    protected float _current = 10e10f, _best = 10e10f;
    protected HashMap<Node, Node> _endEffectorMap = new HashMap<>();

    public TRIKTree(Node root){
        this(root, SimpleTRIK.HeuristicMode.EXPRESSIVE_FINAL);
    }

    public TRIKTree(Node root, SimpleTRIK.HeuristicMode mode){
        super();
        TreeNode dummy = new TreeNode(); //Dummy TreeNode to Keep Reference
        _mode = mode;
        _setup(dummy, root, new ArrayList<Node>());
        //dummy must have only a child,
        this._root = dummy._children().get(0);
        this._root._parent = null;
    }


    protected void _setup(TreeNode parent, Node node, List<Node> list){
        if(node == null) return;
        if(node.children().isEmpty()) { //Is a leaf node, hence we've found a chain of the structure
            list.add(node);
            SimpleTRIK solver = new SimpleTRIK(list, _mode);
            new TreeNode(parent, solver);
        } else if(node.children().size() > 1){
            list.add(node);
            SimpleTRIK solver = new SimpleTRIK(list, _mode);
            TreeNode treeNode = new TreeNode(parent, solver);
            for(Node child : node.children()){
                List<Node> newList = new ArrayList<>();
                //newList.add(node);
                _setup(treeNode, child, newList);
            }
        } else{
            list.add(node);
            _setup(parent, node.children().get(0), list);
        }
    }
    Vector[] _current_coords;
    Vector[] _desired_coords;
    protected boolean _solve(TreeNode treeNode){
        if(treeNode._children == null || treeNode._children.isEmpty()){
            SimpleTRIK solver = treeNode._solver;
            if(solver.target() == null) return false;
            //solve ik for current chain
            solver.reset();
            solver.solve(); //Perform a given number of iterations
            return true;
        }

        int childrenModified = 0;
        SimpleTRIK solver = treeNode._solver;
        List<Vector> current_coords = new ArrayList<Vector>();
        List<Vector> desired_coords = new ArrayList<Vector>();
        for(TreeNode child : treeNode._children()){
            if( _solve(child)){
                childrenModified++;
                Vector o = solver.context().chain().get(solver.context().last()).location(child._solver.context().chain().get(0));
                Vector eff = solver.context().chain().get(solver.context().chain().size() - 1).location(child._solver.context().chain().get(child._solver.context().chain().size() - 1));
                Vector t = solver.context().chain().get(solver.context().chain().size() - 1).location(child._solver.target());
                Vector diff = Vector.subtract(t, eff);
                //current_coords.add(o);
                //current_coords[c] = Vector.subtract(current_coords[c],o);
                //desired_coords.add(Vector.add(o,diff));
                //desired_coords[c] = Vector.subtract(local_t,o);
                current_coords.add(eff.get());
                desired_coords.add(t.get());
            }
        }

        if(childrenModified <= 0) return false;

        //in case a children was modified and the node is not a leaf
        Quaternion rotation = QCP.CalcRMSDRotationalMatrix(desired_coords, current_coords, null);
        solver.context().chain().get(solver.context().chain().size() - 1).rotate(rotation);
        Node target = new Node();
        //solve ik for current chain
        //Set the target position
        Vector translation = new Vector();
        for(int i = 0; i < current_coords.size(); i++){
            Vector o = rotation.rotate(current_coords.get(i));
            translation.add(Vector.subtract(desired_coords.get(i), o)); //TODO: consider weights
        }
        translation.multiply(1f/current_coords.size());
        target.setPosition(solver.context().chain().get(solver.context().chain().size() - 1).worldLocation(translation));

        solver.setTarget(target);
        if(solver.context().chain().size() >= 2){//If the solver has only a node we require to update manually
            solver.reset();
            solver.solve(); //Perform a given number of iterations
        }
        return true;
    }

    @Override
    protected boolean _iterate() {
        _solve(_root);
        return false;
    }

    @Override
    protected void _update() {

    }

    protected boolean _changed(TreeNode treeNode) {
        if (treeNode == null) return false;
        if (treeNode._solver().changed() && treeNode._children().isEmpty()) return true;
        for (TreeNode child : treeNode._children()) {
            if (_changed(child)) return true;
        }
        return false;
    }

    @Override
    protected boolean _changed() {
        return _changed(_root);
    }

    protected void _reset(TreeNode treeNode) {
        if (treeNode == null) return;
        //Update Previous Target
        if (treeNode._solver().changed()) treeNode._solver().reset();
        for (TreeNode child : treeNode._children()) {
            _reset(child);
        }
    }


    @Override
    protected void _reset() {
        _iterations = 0;
        _best = 0;
        _current = 10e10f;
        _reset(_root);
    }

    @Override
    public float error() {
        float e = 0;
        for(Map.Entry<Node, Node> entry : _endEffectorMap.entrySet()){
            e += Vector.distance(entry.getKey().position(), entry.getValue().position());
        }
        return e;
    }

    @Override
    public void setTarget(Node endEffector, Node target) {
        addTarget(endEffector, target);
    }

    public Node head() {
        return (Node) _root._solver().context().chain().get(0);
    }


    protected boolean _addTarget(TreeNode treeNode, Node endEffector, Node target) {
        if (treeNode == null) return false;
        for(Node node : treeNode._solver().context().chain()){
            if (node == endEffector) {
                treeNode._solver().setTarget(target);
                _endEffectorMap.put(endEffector, target);
                return true;
            }
        }
        for (TreeNode child : treeNode._children()) {
            _addTarget(child, endEffector, target);
        }
        return false;
    }

    public boolean addTarget(Node endEffector, Node target) {
        return _addTarget(_root, endEffector, target);
    }


    public void setMaxError(float maxError) {
        super.setMaxError(maxError);
        _setMaxError(maxError, _root);
    }

    protected void _setMaxError(float maxError, TreeNode node){
        node._solver.setMaxError(maxError);
        for(TreeNode child : node._children()){
            _setMaxError(maxError, child);
        }
    }



}
