package ses;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class sesAutomation {

	static ChromeDriver driver;
	static int randomid = (int) (Math.random() * 9999);

	public static void Login() throws InterruptedException {
		driver = new ChromeDriver();
		driver.manage().window().maximize();
		driver.get("https://development-ses.changepond.com/login");
		Thread.sleep(1500);
		driver.findElement(By.id("email")).sendKeys("karthickg@mailinator.com");
		driver.findElement(By.id("password")).sendKeys("Kart$123");
		Thread.sleep(1500);
		driver.findElement(By.xpath("//button[@type='submit']")).click();

	}

	public static void createPersonalDetails() throws InterruptedException {

		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(60));
		driver.findElement(By.xpath("//a[@data-tooltip-content='Manage User']")).click();
		driver.findElement(By.xpath("//button[.='Create User']")).click();
		driver.findElement(By.xpath("//input[@name='personalDetail.employee_id']")).sendKeys(String.valueOf(randomid));
		Thread.sleep(1500);
		driver.findElement(By.xpath("//input[@name='personalDetail.first_name']")).sendKeys(random.randomString(9));
		driver.findElement(By.xpath("//input[@name='personalDetail.last_name']")).sendKeys(random.randomString(9));
		driver.findElement(By.xpath("//input[@name='personalDetail.date_of_birth']"))
				.sendKeys(random.randomDOB(2000, 2005));
		Thread.sleep(1500);
		driver.findElement(By.xpath("//input[@name='personalDetail.gender_id']")).click();
		driver.findElement(By.xpath("//input[@name='personalDetail.contact_email']"))
				.sendKeys(random.randomAlphaNumeric(5) + "@gmail.com");
		Thread.sleep(1500);
		driver.findElement(By.xpath("//input[@name='personalDetail.contact_number']"))
				.sendKeys(random.randomMobileNumber());
		driver.findElement(By.xpath("//input[@name='personalDetail.emergency_contact_number']"))
				.sendKeys(random.randomMobileNumber());
		Thread.sleep(1500);

//		JavascriptExecutor js = (JavascriptExecutor) driver;
//		js.executeScript("window.scrollBy(0,500)");
//		Thread.sleep(1500);

		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

		// Scroll if needed
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);",
				driver.findElement(By.xpath("//button[@name='personalDetail.blood_group_id']")));

		// Click dropdown
		WebElement dropdown = wait.until(
				ExpectedConditions.elementToBeClickable(By.xpath("//button[@name='personalDetail.blood_group_id']")));
		dropdown.click();

		// Select B+
		WebElement option = wait.until(ExpectedConditions
				.elementToBeClickable(By.xpath("//ul[contains(@class,'dropdown-list')]//li[normalize-space()='B+']")));
		option.click();
		System.out.println("Execution completed");
		driver.quit();

	}
}
