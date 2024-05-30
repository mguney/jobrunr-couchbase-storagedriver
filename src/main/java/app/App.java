package app;

import app.user.User;
import app.user.UserRecord;
import app.user.UserRecordNameSurname;
import app.user.UserRepository;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.RecurringJobBuilder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class App implements CommandLineRunner {
    
    private final JobRequestScheduler jobRequestScheduler;
    
    private final UserRepository userRepository;
    
    private String[][] data = {{"Morgan", "Sims", "a@protonmail.ca"}, {"Zephr", "Hebert", "dapibus.id.blandit@aol.couk"}, {"Xaviera", "Rasmussen", "facilisis@aol.ca"},
            {"Quinlan", "Glenn", "vitae@outlook.edu"}, {"Heather", "Williams", "purus.accumsan@yahoo.net"}, {"Stacey", "Kent", "at.arcu@outlook.ca"},
            {"Cooper", "Bishop", "nisi.cum.sociis@google.com"}, {"Shad", "Gibbs", "phasellus@protonmail.net"}, {"Richard", "Pena", "non@icloud.edu"},
            {"Xena", "Chang", "pellentesque.massa.lobortis@icloud.edu"}, {"Jocelyn", "Bartlett", "aliquet.lobortis.nisi@hotmail.couk"},
            {"Chaney", "Davis", "integer.mollis.integer@google.org"}, {"Martena", "Cotton", "quisque.libero@yahoo.ca"}, {"Serena", "Lloyd", "inceptos@icloud.org"},
            {"Chaney", "Edwards", "faucibus.leo@yahoo.com"}, {"Olivia", "Fischer", "varius.et.euismod@outlook.couk"}, {"Quinn", "Wells", "pharetra@protonmail.com"},
            {"Daniel", "Buchanan", "nulla.at@protonmail.net"}, {"Fleur", "Levy", "malesuada.id.erat@yahoo.net"}, {"Levi", "Rosales", "tellus.suspendisse.sed@hotmail.ca"},
            {"Lunea", "Landry", "phasellus@icloud.net"}, {"Donna", "Heath", "eget.massa@google.net"}, {"Allegra", "Goff", "aenean.egestas@outlook.net"},
            {"Montana", "Wyatt", "porttitor.interdum@aol.edu"}, {"Jana", "Figueroa", "dui.lectus@icloud.ca"}, {"Cade", "Steele", "libero.lacus@icloud.net"},
            {"Jenette", "Walsh", "sem.eget@aol.org"}, {"Adrian", "Bowers", "dolor.nulla.semper@icloud.edu"}, {"Dai", "Griffith", "dis.parturient@protonmail.com"},
            {"Paul", "Delgado", "lacus@yahoo.couk"}};
    
    
    public void createUser() {
        Random random = new Random();
        int index = random.nextInt(data.length);
        User save = userRepository.save(User.builder().firstName(data[index][0] + random.nextInt(10)).lastName(data[index][1] + random.nextInt(10)).email(data[index][2]).build());
        System.out.println(save);
    }
    
    @Override
    public void run(String... args) throws Exception {
        BackgroundJob.enqueue(UUID.randomUUID(), this::createUser);
        BackgroundJob.enqueue(UUID.randomUUID(), this::createUser);
        BackgroundJob.enqueue(UUID.randomUUID(), this::createUser);
        
        BackgroundJob.schedule(UUID.randomUUID(), ZonedDateTime.now().plusSeconds(10), service -> this.createUser());
        BackgroundJob.schedule(UUID.randomUUID(), ZonedDateTime.now().plusSeconds(20), service -> this.createUser());
        
        BackgroundJob.createRecurrently(RecurringJobBuilder.aRecurringJob()
                                                           .withId(UUID.randomUUID().toString())
                                                           .withCron("0/5 * * * * *")
                                                           .withLabels("asd", "dds", "asdas")
                                                           .withName("testJobName")
                                                           .withDetails(service -> createUser()));
        
        List<User> richard = userRepository.findAllByFirstNameStartsWith("Richard");
        System.out.println(richard);
        List<UserRecordNameSurname> richard2 = userRepository.findAllByLastNameStartsWith("Pena");
        System.out.println(richard2);
        List<UserRecord> richard3 = userRepository.countByFirstName();
        System.out.println(richard3);
    }
}
