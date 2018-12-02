package JavaAsServiceLib;

import CommonLib.Common;
import CommonLib.ConsoleInputInterpreter;
import CommonLib.XmlSettingsBase;
import java.util.ArrayList;
import java.util.Objects;

/**
 *
 * 
 * 
 * parent class of main java-handler class (with witch handler to be started with procrun or jsvc.
 * 
 * class must contain static constructor:
 * {@code CommonsDaemonService.setSingleton(new descendant_of_CommonsDaemonService())};
 * 
 * on windows (procrun) use {@code OnStart} and {@code OnStop} methods
 * 
 * on linux (jsvc) use {@code start} and {@code stop} methods 
 * 
 */
public abstract class CommonsDaemonService {
    public final String jarFileName;
    final String[] requiredSettingsFromFile;
    final protected XmlSettingsBase settingsFromFile;
    protected CommonsDaemonService(String jarFileName)
    {
        this(jarFileName, null);
    }
    protected CommonsDaemonService(String jarFileName, String[] requiredSettingsFromFile)
    {
        this.jarFileName = jarFileName;
        this.requiredSettingsFromFile = requiredSettingsFromFile;
        this.settingsFromFile = requiredSettingsFromFile == null || requiredSettingsFromFile.length == 0 ? null : new XmlSettingsBase(jarFileName, jarFileName + ".settings", true, requiredSettingsFromFile);
        System.out.println("JavaAsServiceLib.CommonsDaemonService.<init>() StackTrace: " + Common.getStackTrace(0));
    }

    private ConsoleInputInterpreter.CommandHandler[] consoleCommands;
    protected final void setConsoleCommands(ConsoleInputInterpreter.CommandHandler[] consoleCommands) { this.consoleCommands = consoleCommands; }

    
    private static volatile Class<? extends CommonsDaemonService> singletonClass;
    private static volatile CommonsDaemonService singleton;
    private static final Object singletonLOCK = new Object();
    protected static void setSingletonClass(Class<? extends CommonsDaemonService> c) 
    {
        synchronized (singletonLOCK)
        {
            if (singletonClass != null)
                throw new Error("MUSTNEVERTHROW: setSingletonClass() called again!");
            singletonClass = c; 
        }
    }
    private static void setSingleton() 
    {                 
        try {
            setSingleton(singletonClass.newInstance());
        } catch (Exception ex) {
            throw new Error("MUSTNEVERTHROW: error calling default constructor: " + ex.toString());
        }
    }
    private static void setSingleton(CommonsDaemonService v) 
    { 
        synchronized (singletonLOCK)
        {
            if (singleton != null)
                throw new Error("MUSTNEVERTHROW: setSingleton() called again!");
            singleton = v; 
        }
    }
    private static void chkSingletonClass(Class<? extends CommonsDaemonService> c)
    {
        if (singletonClass == null)
            throw new Error("MUSTNEVERTHROW: descendant class must have static constructor containing setSingletonClass(<descendant class>), e.g.:\r\n" +
                " static { setSingletonClass(my_descendant_of_CommonsDaemonService.class); } \r\n\r\n"
            );   
        if (c != null && c != singletonClass)
            throw new Error("MUSTNEVERTHROW: descendant class singletonClass (" + singletonClass.getCanonicalName() + "), is not the class used to start the service (" + c.getCanonicalName() + ")!\r\n"
            );  
    }
    
    
    public static void main(String[] args) 
    {
        String m = "Run only as windows/linux-service!";
        System.err.println(m);
        throw new Error(m);
    }
    
    /**
     * for windows-service
     * @param args
     */
    public static void OnStart(String [] args){
        chkSingletonClass(null);
        
        setSingleton();
        
        singleton.serviceStart_internal();
        
        singleton.startServiceWorker_and_runMainServiceCycle();
    }
    /**
     * for windows-service
     * @param args
     */
    public static void OnStop(String [] args){
        singleton.serviceStop_internal();
    }
        

    /**
     * for linux-service
     * (not in use)
     * @param arguments
     */
    public final void init(String[] arguments) {
        //
    }
    
    /**
     * for linux-service
     */
    public final void start() {           
        chkSingletonClass(this.getClass());
        
        setSingleton(this);
        
        singleton.serviceStart_internal();

        Thread t = new Thread(() -> 
        { 
            singleton.startServiceWorker_and_runMainServiceCycle(); 
        });
        t.setName("CommonsDaemonService main thread");                
        t.start();
    }
    /**
     * for linux-service
     */
    public final void stop() {
        singleton.serviceStop_internal();
    }
    /**
     * for linux-service
     * (not in use)
     */
    public final void destroy() {
        //
    }
    
    
    
    
    
    
    
    
    
    
   
    
    
    
    
    public static class CommonsDaemonServiceStartInfo 
    { 
        public final Common.Log svcmsglog;  public final Common.Log svcerrlog;  public final ServiceWorkerThread worker; 
        /**
        * Init class {@code CommonsDaemonService} 
        * 
        * @param svcmsglog Service log
        * @param svcerrlog Service error log
        * @param worker {@code ServiceWorkerThread} buisness-logic container. Runs in separate thread.
        */
        public CommonsDaemonServiceStartInfo(Common.Log svcmsglog, Common.Log svcerrlog, ServiceWorkerThread worker) 
        {   this.svcmsglog = Objects.requireNonNull(svcmsglog, "MUSTNEVERTHROW: svcmsglog is null!"); 
            this.svcerrlog = Objects.requireNonNull(svcerrlog, "MUSTNEVERTHROW: svcerrlog is null!"); 
            this.worker =    Objects.requireNonNull(worker,    "MUSTNEVERTHROW: worker is null!"); } 
    }
    private CommonsDaemonServiceStartInfo si;
    private void serviceStart_internal()
    {
        System.out.println(Common.NowToString() + "    " + "Service process started");
        si = onServiceStart();
        if (si == null)
            throw new Error("MUSTNEVERTHROW: onServiceStart() must return filled CommonsDaemonServiceStartInfo, not null!");
        si.svcmsglog.write("CommonsDaemonService.serviceStart", "Service process init complete.");
    }    
    
    
    private void serviceStop_internal() {
        si.svcmsglog.write("CommonsDaemonService.serviceStop", "Service stopping by OS.");
        if (onServiceStopping != null) 
            try { onServiceStopping.call(); } catch (Throwable th) { si.svcerrlog.write(th, Common.getCurrentSTE(), "Error onServiceStopping"); }
        serviceStopWait();
        if (onServiceStopped != null) 
            try { onServiceStopped.call(); } catch (Throwable th) { si.svcerrlog.write(th, Common.getCurrentSTE(), "Error onServiceStopped"); }
        si.svcmsglog.write("CommonsDaemonService.serviceStop", "Service stopping by OS: service stopped.", true);
        System.out.println(Common.NowToString() + "    " + "Service stopping by OS: service stopped.");
    }

    
    
    
    
    private void startWorker() throws Exception
    {
        si.worker.start();
    }
    public void worker_signalStop()
    {
        si.worker.signalStop();
    }
    private boolean isWorkerStopped()
    {
        return si.worker.isStopped();
    }
    public boolean worker_hasStopSignal()
    {
        return si.worker.hasStopSignal();
    }
    public Common.Log worker_msgLog()
    {
        return si.worker.workermsgLog;
    }
    public Common.Log worker_excLog()
    {
        return si.worker.workerexcLog;
    }
    public void worker_addIsStopped_addon(Common.Func<Boolean> isStopped_addon)
    {
        si.worker.isStopped_addon.add(isStopped_addon);
    }
    public void worker_addOnSignalStop(Common.Action onSignalStop)
    {
        si.worker.onSignalStop.add(onSignalStop);
    }
    /**
     * service initialization CommonsDaemonServiceStartInfo
     * 
     * @return intiated objects {@code CommonsDaemonService} 
     */
    protected abstract CommonsDaemonServiceStartInfo onServiceStart();
    protected Common.Action onServiceStopping;
    protected Common.Action onServiceStoppingTimeout;
    protected Common.Action onServiceStopped;
    
   
    private volatile boolean mainServiceCycle_running;
    private volatile boolean serviceStoping;
    private Long scheduledExit;
    private final long scheduledExitTimeOut_sec = 5 * 60;
    private void startServiceWorker_and_runMainServiceCycle()
    {
        try
        {
            ConsoleInputInterpreter.start(Common.ConcatArray(new ConsoleInputInterpreter.CommandHandler[] {
                new ConsoleInputInterpreter.CommandHandler("exit", "Correct exit", false, (a)->
                {
                    Common.setTimeout(()->
                    {
                        try
                        {
                            serviceStopWait();
                        }
                        finally
                        {
                            serviceExitAndTryRestart(false, "exit command entered");
                        }    
                    }, 0, true);
                }),                
                new ConsoleInputInterpreter.CommandHandler("vars", "Show handler vars", false, (a)->
                {
                    System.out.println(Common.getAllFieldValues(this, "(" + jarFileName + ")"));
                })
            }, consoleCommands));
        }
        catch (Throwable th) {
            si.svcerrlog.write(th, Common.getCurrentSTE(), "error starting ConsoleInputInterpreter (ConsoleInputInterpreter.start)");
        }

        try
        {
            startWorker();
        }
        catch (Throwable th)
        {
            try
            {
                serviceStopWait();
            }
            finally
            {
                serviceExitAndTryRestart(true, "error starting service worker thread: " + th.toString() + "\r\n\r\n" + Common.getGoodStackTrace(th, 0));
            }            
        }
        
        try
        {
            mainServiceCycle_running = true;
            while(!serviceStoping)
            {    
                if (isWorkerStopped())
                    serviceExitAndTryRestart(false, "service worker thread interrupded.");
                
                if(scheduledExit == null && worker_hasStopSignal())
                    scheduledExit = System.currentTimeMillis();
                
                if (scheduledExit != null && (scheduledExit + (scheduledExitTimeOut_sec * 1000)) < System.currentTimeMillis())
                    serviceExitAndTryRestart(true, "stop-signal to service worker thread was sent, but it didnt stop in " + scheduledExitTimeOut_sec + " sec! Threads stacks: " + Common.getAllStackTraces());
                
                Thread.sleep(100);
            }
        }
        catch(Throwable th)
        {
            serviceExitAndTryRestart(true, "service worker thread main loop error: " + th.toString() + "\r\n\r\n" + Common.getGoodStackTrace(th, 0));
        }
        finally
        {
            mainServiceCycle_running = false;
        }      
    }
    private void serviceStopWait()
    {
        serviceStoping = true;
        try { Thread.sleep(200); } catch (Throwable ex) { }
        worker_signalStop();
        for(int n = 0; mainServiceCycle_running || !isWorkerStopped(); n++)
        {
            if (n > 30 * 100)
            {
                si.svcerrlog.write("CommonsDaemonService.serviceStopWait", "service stopping timeout (mainServiceCycle_running=" + mainServiceCycle_running + ", isWorkerStopped()=" + isWorkerStopped() + ").", new String[][]{new String[]{"AllStackTraces", Common.getAllStackTraces()}} );
                if (onServiceStoppingTimeout != null) 
                    try { onServiceStoppingTimeout.call(); } catch (Throwable th) { si.svcerrlog.write(th, Common.getCurrentSTE(), "onServiceStoppingTimeout error"); }
                break;
            }
            try { Thread.sleep(10); } catch (Throwable ex) { }
        }
    }
    private void serviceExitAndTryRestart(boolean iserr, String message)
    {
        try
        {
            String logmsg = "Service exit with restart signal" + (message != null ? " because of: " + message : ".");
            (iserr ? si.svcerrlog : si.svcmsglog).write("CommonsDaemonService.serviceExitAndTryRestart", logmsg, true);
            (iserr ? System.err : System.out).println(Common.NowToString() + "    " + logmsg);
        }
        finally
        {
            System.exit(123);
        }
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    public static final class Worker_DoWork_resultFlags { public final boolean doFastLoop; 
        public Worker_DoWork_resultFlags(boolean doFastLoop) { this.doFastLoop = doFastLoop; }
    }
    
    /**
     *
     * 
     * 
     * Class containing buisness-logic in {@code DoWork()} method.
     * 
     * 
     */
    public static abstract class ServiceWorkerThread implements Runnable {
        protected final String serviceName;
        protected final Common.Log workermsgLog;
        protected final Common.Log workerexcLog;
        private final int normalLoopSleep_sec;
        private final int exceptionLoopSleep_sec;
        protected ServiceWorkerThread(String serviceName, Common.Log workermsgLog, Common.Log workerexcLog)
        {
            this(serviceName, workermsgLog, workerexcLog, 0, 0);
        }
        protected ServiceWorkerThread(String serviceName, Common.Log workermsgLog, Common.Log workerexcLog, int normalLoopSleep_sec, int exceptionLoopSleep_sec)
        {
            this.serviceName = serviceName;
            this.workermsgLog = workermsgLog;
            this.workerexcLog = workerexcLog;
            this.normalLoopSleep_sec = normalLoopSleep_sec <= 0 ? 10 : normalLoopSleep_sec;
            this.exceptionLoopSleep_sec = exceptionLoopSleep_sec <= 0 ? 10 * 60 : exceptionLoopSleep_sec;
        }
        private volatile Thread currentThread;
        public final Thread CurrentThread()
        {
            return currentThread;
        }
        private final Object startLOCK = new Object();
        public void start() throws Exception
        {
            synchronized (startLOCK)
            {
                if (currentThread != null)
                    throw new Error("MUSTNEVERTHROW: ServiceWorkerThread.start() method called again!");

                JustBeforeStart();

                if (hasStopSignal())
                {
                    workermsgLog.write("ServiceWorkerThread.start", "Service worker thread \"" + serviceName + "\" was not started.", true);
                }
                else
                {
                    currentThread = new Thread(this);
                    currentThread.setName("ServiceWorkerThread thread");
                    currentThread.start();
                    for (int n = 0; !isRunned; n++)
                    {
                        if (n >= 30 * 10)
                            throw new Error("ServiceWorkerThread.run() didnt start in 30 sec!");
                        try { Thread.sleep(100); } catch (InterruptedException iex) { throw new RuntimeException(iex); }
                    }
                }
            }
        }

        protected abstract void JustBeforeStart() throws Exception;
        protected abstract Worker_DoWork_resultFlags DoWork() throws Exception;
        protected abstract void DoWorkOnException(Exception ex, StackTraceElement exSte);

        private String errorMessage = null;
        public String errorMessage()
        {
            return errorMessage; 
        }
        private volatile boolean doStop;
        private volatile boolean isRunned;
        private volatile boolean isStopped;
        public boolean isStopped()
        {
            if (!isStopped_addon.isEmpty())
                for (Common.Func<Boolean> e : isStopped_addon)
                    if (!e.call())
                        return false;
            return isStopped || !isRunned;
        }
        public final  ArrayList<Common.Func<Boolean>> isStopped_addon = new ArrayList<>();
        private volatile boolean inwork;
        public boolean isInwork()
        {
            return inwork;
        }
        public final ArrayList<Common.Action> onSignalStop = new ArrayList<>();
        public void signalStop()
        {
            if (!onSignalStop.isEmpty())
                try { onSignalStop.forEach((e) -> { e.call(); }); } catch (Exception ex) { workerexcLog.write(ex, Common.getCurrentSTE(), "Error onSignalStop()", true); }
            doStop = true;
        }
        public boolean hasStopSignal()
        {
            return doStop;
        }
        @Override
        public final void run() {
            if (currentThread == null)
                throw new Error("MUSTNEVERTHROW:  run method of ServiceWorkerThread must not be called directly! Use ServiceWorkerThread.start().");
            try
            {
                isRunned = true;

                workermsgLog.write("ServiceWorkerThread.run", "Service worker thread \"" + serviceName + "\" started");
                while(!hasStopSignal())
                {
                    try
                    {
                        inwork = true;

                        Worker_DoWork_resultFlags resultFlags = DoWork();

                        inwork = false;
                        for (int n = 0; n <= ((resultFlags != null && resultFlags.doFastLoop ? 2 : normalLoopSleep_sec * 10)); n++)
                        {
                            Thread.sleep(100);
                            if (hasStopSignal())
                                return;
                        }
                    } 
                    catch (Exception ex) 
                    {
                        if (ex instanceof StopException)
                            return;
                        workerexcLog.write(ex, Common.getCurrentSTE(), "Error DoWork()");
                        try { DoWorkOnException(ex, Common.getCurrentSTE()); } catch (Exception exex) { workerexcLog.write(exex, Common.getCurrentSTE(), "Error DoWorkOnException(e)"); }
                        inwork = false;
                        for (int n = 0; n <= (exceptionLoopSleep_sec * 10); n++)
                        {
                            try { Thread.sleep(100); } catch (InterruptedException iex) {}
                            if (hasStopSignal())
                                return;
                        }
                    } 
                    finally 
                    {
                        inwork = false;
                    }   
                }
            } 
            catch (Throwable th) 
            {
                if (th instanceof Common.ArtificialError)
                    errorMessage = th.getMessage();
                else
                    errorMessage = th.toString().trim() + "\r\n\r\n" + Common.getGoodStackTrace(th, 0);
                workerexcLog.write(th, Common.getCurrentSTE(), new String[][]{new String[]{"fatalError","true"}});
            } 
            finally 
            {
                try { onThreadExit(); } catch(Throwable th) { workerexcLog.write(th, Common.getCurrentSTE(), "Ошибка onThreadExit()", true); }
                finally
                {
                    try
                    {
                        workermsgLog.write("ServiceWorkerThread.run", "Рабочий поток сервиса \"" + serviceName + "\" остановлен" + (errorMessage != null ? " из-за критической ошибки!" : "."), true);
                    }
                    finally
                    {
                        isStopped = true;
                    }
                }
            }            
        }
        public class StopException extends Exception { };

        protected abstract void onThreadExit() throws Exception;





    }
}
