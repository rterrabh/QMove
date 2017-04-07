package qmove.movemethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import net.sourceforge.metrics.core.Metric;
import net.sourceforge.metrics.core.sources.AbstractMetricSource;
import net.sourceforge.metrics.core.sources.Dispatcher;
import qmove.utils.qMooveUtils;

public class MoveMethods {
	
	ArrayList<ClassMethod> methods;
	ArrayList<ClassMethod> methodsCanBeMoved;
	ArrayList<MethodsChosen> methodsMoved;
	ArrayList<Recommendation> listRecommendations;
	ArrayList<IMethod> iMethod;
	Map<String, ArrayList<IMethod>> allMethods;
	MethodsTable methodsTable;
	Metric[] metricsOriginal;
	AbstractMetricSource ms;
	IJavaElement jee;
	IJavaProject project;
	
	
	public MoveMethods(IJavaProject project, ArrayList<Recommendation> listRecommendations){
		this.project = project;
		methods = new ArrayList<ClassMethod>();
		methodsMoved = new ArrayList<MethodsChosen>();
		this.listRecommendations = listRecommendations;
		methodsCanBeMoved = new ArrayList<ClassMethod>();
		iMethod = new ArrayList<IMethod>();
	}
	
	public ArrayList<Recommendation> moveMethods() throws ExecutionException {
			
		allMethods = qMooveUtils.getClassesMethods(project.getProject());
		getMethodsClone();
			
		ms = Dispatcher.getAbstractMetricSource(project.getPrimaryElement());
	    metricsOriginal = QMoodMetrics.getQMoodMetrics(ms);
		
	    
	    		
		MethodsChosen aux;
		int qmoveID = 0;
	    
		while(listRecommendations.size() > 0){
			
			for(int i=0; i<methodsCanBeMoved.size(); i++){
	    	
				aux = checkMove.startRefactoring(methodsCanBeMoved.get(i), metricsOriginal);
				if(aux != null) methodsMoved.add(aux);		
			}
			
			Metric[] auxMetrics = metricsOriginal;
			
			Collections.sort (methodsMoved, new Comparator() {
	            public int compare(Object o1, Object o2) {
	                MethodsChosen m1 = (MethodsChosen) o1;
	                MethodsChosen m2 = (MethodsChosen) o2;
	                return m1.calculePercentage(auxMetrics) > m2.calculePercentage(auxMetrics) ? -1 : (m1.calculePercentage(auxMetrics) < m2.calculePercentage(auxMetrics) ? +1 : 0);
	            }
	        });
			
			ArrayList<MethodsChosen> clone = new ArrayList<MethodsChosen>(methodsMoved.size());
			    for (MethodsChosen item : methodsMoved){
					try {
						clone.add(item.clone());
					} catch (CloneNotSupportedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
			}
			
			methodsTable = new MethodsTable(auxMetrics, clone);
			
			metricsOriginal = methodsMoved.get(0).getMetrics();
			
			try {
				methodsMoved.get(0).move();
			} catch (OperationCanceledException | CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			methodsCanBeMoved.removeIf(methodsCanBeMoved -> methodsCanBeMoved.getMethod() == methodsMoved.get(0).getMethod());
			
			listRecommendations.add(new Recommendation (++qmoveID, methodsTable, methodsMoved.get(0), methodsMoved.get(0).calculePercentage(auxMetrics), getMethod(methodsMoved.get(0).getMethod())));
			
			methodsMoved.removeAll(methodsMoved);
			
			
		}
	            
	    
	    try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("qmove.views.QMoveView");
		} catch (PartInitException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	    
	    
	    IProgressMonitor m = new NullProgressMonitor();
	    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
	    IProject project = workspaceRoot.getProject("Temp");
	    try {
			project.delete(true, m);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	   
		return null;
	}
	
	public void getMethodsProject(IJavaElement je) throws JavaModelException {
		IJavaProject project = je.getJavaProject();
	    p = (IProject)project.getResource();
	    if (project.isOpen()) {
	    	IPackageFragment[] packages = project.getPackageFragments();
		      for (IPackageFragment mypackage : packages) {
		        if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
		          for (ICompilationUnit unit : mypackage.getCompilationUnits()) {
		             IType[] types = unit.getTypes();
		             for (int i = 0; i < types.length; i++) {
		               IType type = types[i];
		               IMethod[] imethods = type.getMethods();
		               for(int j=0; j<imethods.length; j++)
		            	   iMethod.add(imethods[j]);
		             }
		          }
		        }
		      }
	    }
	}
		
	
	
	public void getMethodsClone() throws CoreException, InterruptedException{
	    IJavaProject projectTemp = JavaCore.create(cloneProject());
	    Thread.sleep(10000);
	    jee = projectTemp.getPrimaryElement();
	    if (projectTemp.isOpen()) {
	    	IPackageFragment[] packages = projectTemp.getPackageFragments();
		      for (IPackageFragment mypackage : packages) {
		        if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
		          for (ICompilationUnit unit : mypackage.getCompilationUnits()) {
		             IType[] types = unit.getTypes();
		             for (int i = 0; i < types.length; i++) {
		               IType type = types[i];
		               IMethod[] imethods = type.getMethods();
		               for(int j=0; j<imethods.length; j++)
		            	   methods.add(new ClassMethod(mypackage, type, imethods[j]));
		             }
		          }
		        }
		      }
		}
		
	}
	
	public IProject cloneProject() throws CoreException{
		IProgressMonitor m = new NullProgressMonitor();
	    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
	    IProjectDescription projectDescription = p.getDescription();
	    String cloneName = "Temp";
	    // create clone project in workspace
	    IProjectDescription cloneDescription = workspaceRoot.getWorkspace().newProjectDescription(cloneName);
	    // copy project files
	    p.copy(cloneDescription, true, m);
	    IProject clone = workspaceRoot.getProject(cloneName);
	    
	    cloneDescription.setNatureIds(projectDescription.getNatureIds());
	    cloneDescription.setReferencedProjects(projectDescription.getReferencedProjects());
	    cloneDescription.setDynamicReferences(projectDescription.getDynamicReferences());
	    cloneDescription.setBuildSpec(projectDescription.getBuildSpec());
	    cloneDescription.setReferencedProjects(projectDescription.getReferencedProjects());
	    clone.setDescription(cloneDescription, null);
	    clone.open(m);
	    return clone;
	}
	
	public IMethod getMethod(IMethod method){
		
		String methodOriginal;
		String classOriginal;
		String methodClone = method.getElementName();
		String classClone = method.getCompilationUnit().getParent().getElementName() + "." + method.getDeclaringType().getElementName();
		
		for (Map.Entry<String, ArrayList<IMethod>> entrada : allMethods.entrySet()) {
			
			classOriginal = entrada.getKey();
			
			for(int i=0; i<entrada.getValue().size(); i++){
				
				methodOriginal = entrada.getValue().get(i).getElementName();
				
				if(classClone.compareTo(classOriginal) == 0
					&& methodClone.compareTo(methodOriginal) == 0){
				
					return entrada.getValue().get(i);
				}
			}
		}
		
		return null;
		
	}
}