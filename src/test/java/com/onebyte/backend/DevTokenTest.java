import com.onebyte.backend.global.jwt.JwtProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DevTokenTest {

    @Autowired
    JwtProvider jwtProvider;

    @Test
    void printToken() {
        System.out.println("ACCESS TOKEN = " + jwtProvider.createAccessToken(1L));
    }
}