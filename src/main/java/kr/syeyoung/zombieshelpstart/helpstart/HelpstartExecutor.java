package kr.syeyoung.zombieshelpstart.helpstart;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.stream.Collectors;
import kr.syeyoung.zombieshelpstart.bot.BotProvider;

public class HelpstartExecutor extends Thread {
    private final Queue<HelpstartRequest> prioritizedHelpstartSessionQueue = new PriorityQueue<>(Comparator.<HelpstartRequest>comparingInt((o) -> o.getUsernames().size()).reversed());
    private final Queue<HelpstartRequest> normalHelpstartSessionQueue = new PriorityQueue<>(Comparator.<HelpstartRequest>comparingInt((o) -> o.getUsernames().size()).reversed());
    private static final HelpstartExecutor helpstartExecutor = new HelpstartExecutor();

    private HelpstartSession currentSession = null;

    public HelpstartExecutor() {
    }

    public List<String> getRequests() {
        return this.normalHelpstartSessionQueue.stream().filter((h) -> !h.isCanceled()).map((h) -> h.getUsernames().size() + " players requested by " + h.getRequester().getEffectiveName()).collect(Collectors.toList());
    }

    public void cancelAll() {
        synchronized(BotProvider.getInstance()) {
            this.normalHelpstartSessionQueue.forEach((hr) -> hr.setCanceled(true));
            updateHurry();

            BotProvider.getInstance().notifyAll();
        }
    }

    public static HelpstartExecutor getInstance() {
        return helpstartExecutor;
    }

    public void addToQueue(HelpstartRequest helpstartRequest) {
        synchronized(BotProvider.getInstance()) {
            this.normalHelpstartSessionQueue.add(Objects.requireNonNull(helpstartRequest));
            this.prioritizedHelpstartSessionQueue.add(Objects.requireNonNull(helpstartRequest));
            updateHurry();

            BotProvider.getInstance().notifyAll();
        }
    }

    public void updateHurry() {
        if (currentSession != null) {
            synchronized (BotProvider.getInstance()) {
                currentSession.setHurry(BotProvider.getInstance().getAvailableBots() < 3);
            }
        }
    }

    public void run() {
        synchronized(BotProvider.getInstance()) {
            while (true) {
                try {
                    while (this.normalHelpstartSessionQueue.isEmpty()) {
                        try {
                            BotProvider.getInstance().wait();
                        } catch (InterruptedException var10) {
                            var10.printStackTrace();
                        }
                    }

                    HelpstartRequest hr;
                    this.currentSession = null;
                    if (BotProvider.getInstance().getAvailableBots() < 3) {
                        while (this.prioritizedHelpstartSessionQueue.peek().getUsernames().size() + BotProvider.getInstance().getAvailableBots() < 4 && !this.prioritizedHelpstartSessionQueue.peek().isCanceled()) {
                            try {
                                BotProvider.getInstance().wait();
                            } catch (InterruptedException var12) {
                                var12.printStackTrace();
                            }
                        }

                        hr = Objects.requireNonNull(this.prioritizedHelpstartSessionQueue.poll());
                        this.normalHelpstartSessionQueue.remove(hr);
                    } else {
                        while (this.normalHelpstartSessionQueue.peek().getUsernames().size() + BotProvider.getInstance().getAvailableBots() < 4 && !this.normalHelpstartSessionQueue.peek().isCanceled()) {
                            try {
                                BotProvider.getInstance().wait();
                            } catch (InterruptedException var11) {
                                var11.printStackTrace();
                            }
                        }

                        hr = Objects.requireNonNull(this.normalHelpstartSessionQueue.poll());
                        this.prioritizedHelpstartSessionQueue.remove(hr);
                    }

                    if (!hr.isCanceled()) {
                        this.currentSession = new HelpstartSession(hr);
                        hr.setSession(this.currentSession);
                    }
                } catch (Exception var13) {
                    var13.printStackTrace();
                }
            }
        }
    }

    static {
        helpstartExecutor.start();
    }
}
