package com.smart.controller;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.helper.Message;

//@ComponentScan(basePackages = "com.smart")
@Controller
public class HomeController {
	@Autowired
	private BCryptPasswordEncoder PasswordEncoder;
	
	@Autowired
	private UserRepository userRepository;
	
	
	//home handler
	@GetMapping("/")
	public String home(Model model) {
		model.addAttribute("title", "Home - Smart Contact Manager");
		return "home";
	}
	
	//about handler
	@GetMapping("/about")
	public String about(Model model) {
		model.addAttribute("title", "About - Smart Contact Manager");
		return "about";
	}
	
	//signup handler
	@GetMapping("/signup")
	public String signup(Model model) {
		model.addAttribute("title", "signup - Smart Contact Manager");
		model.addAttribute("user", new User());
		return "signup";
	}
	
	//handler for register user
	@PostMapping("/do_register")
	public String registerUser(@Valid @ModelAttribute("user") User user,BindingResult result1,
			@RequestParam(value = "agreement",defaultValue = "false") boolean agreement,Model model,
		HttpSession session){
		
		try {
			
			if (!agreement) {
				System.out.println("you have not agreed terms and conditions");
				throw new Exception("you have not agreed terms and conditions");
			}
			
			if (result1.hasErrors()) {
				
				System.out.println("ERROR"+result1.toString());
				model.addAttribute("user", user);//retrun to form
				return "signup";
			}
			
			user.setRole("ROLE_USER");
			user.setEnabled(true);
			user.setImageUrl("default.png");
			user.setPassword(PasswordEncoder.encode(user.getPassword()));
			

			System.out.println("Agreement "+agreement);
			System.out.println("User :"+user);
			
			User result = this.userRepository.save(user); 
			
			model.addAttribute("user",new User());
			
			session.setAttribute("message", new Message("Successfully Registered", "alert-success"));
			return "signup";
			
		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("user", user);
			session.setAttribute("message", new Message("Somthing Went Wrong !!"+e.getMessage(), "alert-danger"));
			return "signup"; 
		}
		
		
	}
	
	
	//handler for Coustom login
	@GetMapping("/signin")
	public String customLogin(Model model){
				
		model.addAttribute("title", "sigin - Smart Contact Manager");
		return "login";
	}
			
	
	

	
}
