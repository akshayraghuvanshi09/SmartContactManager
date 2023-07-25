package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepositry;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller

@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContactRepositry contactRepositry;
	
	@Autowired
	private BCryptPasswordEncoder PasswordEncoder;
	
	
	//method for adding common data to response
	@ModelAttribute
	public void addCommonData(Model model,Principal  principal) {
		String userName = principal.getName();
		System.out.println(userName);
		
		//get the user using  username(Email)
		
		User user = userRepository.getUserByUserName(userName);
		
		System.out.println("USER"+user);
		
		model.addAttribute("user", user);
		
	}
	
		//dashboard home
		@GetMapping("/index")
		public String dashboard(Model model,Principal principal) {
			
			model.addAttribute("title", "User Dashboard");
			return "normal/user_dashboard";
		}
		
		
		//open add form handler
		@GetMapping("/add-contact")
		public String openAddContactForm(Model model) {
			
			model.addAttribute("title", "Add Contact");
			model.addAttribute("contact", new Contact());
			return "normal/add_contact_form";
		}
		
		//processing add contact form
		@PostMapping("/process-contact")
		public String processContact(@Valid 
				@ModelAttribute Contact contact,
				BindingResult result2,
				@RequestParam("profileImage")
				MultipartFile file, Principal principal , HttpSession session,Model model) {
			
			///
				if (result2.hasErrors()) {
				
				System.out.println("ERROR"+result2.toString());
				model.addAttribute("contact", contact);//retrun to contact form
				return "normal/add_contact_form";
				}
		/////
		
		 try {
			String name = principal.getName();
			
			User user = this.userRepository.getUserByUserName(name);
			
			
			//processing and uploading file
			
			if (file.isEmpty()) {
				//if file is enpty then try your message
				System.out.println("file is empty");
				contact.setImage("contact.png");
			}else {
				//upload file to folder and update the name to contact
				contact.setImage(file.getOriginalFilename());
				
				File saveFile = new ClassPathResource("static/img").getFile();
				
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				///
				model.addAttribute("filePath", path);
				///
				System.out.println(path);
				
				Files.copy(file.getInputStream(), path,StandardCopyOption.REPLACE_EXISTING);
				
				System.out.println("image is uploaded");
			}
	
		
			
			user.getContacts().add(contact);
			
			contact.setUser(user);
			
			this.userRepository.save(user);
			
			System.out.println("Data"+ contact);
			
			System.out.println("added to database");
			
			//message for success 
			session.setAttribute("message",new Message("Your Contact is added !! Add more ..", "success"));
			
		 }catch (Exception e) {
			// TODO: handle exception
			 System.out.println("Error"+e.getMessage());
			 e.printStackTrace();
			 //error message 
			 session.setAttribute("message",new Message("Somthing went wrong  !! try again", "danger"));
		}
			return "normal/add_contact_form";
		}
		
		//show contacts handler
		//per page =5[n] (contact par page)
		//current page= 0[page]
		@GetMapping("/show_contacts/{page}")
		public String showContacts(@PathVariable("page") Integer page,Model m,Principal principal) {
			
			String userName = principal.getName();
			User user = this.userRepository.getUserByUserName(userName);
			
			Pageable pageable = PageRequest.of(page, 5);
			
			Page<Contact> contacts = this.contactRepositry.findContactsByUser(user.getId(),pageable);
			
			m.addAttribute("contacts", contacts);
			m.addAttribute("currentPage",page);
			m.addAttribute("totalPages", contacts.getTotalPages());
			
			m.addAttribute("title","Show User Contacts" );
			//send contact list to ui
			
			
		
			return "normal/show_contacts";
			
			
		}
		
		//showing particular user details
		
		@GetMapping("/{cID}/contact")
		public String showContactDetail(@PathVariable("cID") Integer cID,Model model,Principal principal) {
			
			System.out.println("CID"+cID);
			
			Optional<Contact> contactOptional = this.contactRepositry.findById(cID);
			
			Contact contact = contactOptional.get();
			
			// 
			String userName = principal.getName();
			User user = this.userRepository.getUserByUserName(userName);
			
			if (user.getId()==contact.getUser().getId()) {
				
				model.addAttribute("contact", contact);
				model.addAttribute("title",contact.getName());
			}
			
			
			
			return "normal/contact_detail";
		}
		
		//delete contact handler
		
		@GetMapping("/delete/{cID}")
		@Transactional
		public String deleteContact(@PathVariable("cID") Integer cID,
				Model model,
				HttpSession session,Principal principal) {
			
			Optional<Contact> contactOptional = contactRepositry.findById(cID);
			
			Contact contact = contactOptional.get();
			
			//check...Assignment
			
			
			User user = userRepository.getUserByUserName(principal.getName());
			
			user.getContacts().remove(contact);
			
			userRepository.save(user);
			
			
			
			session.setAttribute("message", new Message("contact deleted succesfully...", "success"));
			
			return "redirect:/user/show_contacts/0";
		}
		
		//opern update form handler
		@PostMapping("/update-contact/{cID}")
		public String updateForm(@PathVariable("cID") Integer cID,Model model) {
			
			model.addAttribute("title", "Update Contact");
			
			Contact contact = contactRepositry.findById(cID).get();
			
			model.addAttribute("contact", contact);
			
			return "normal/update_form";
		}
		
		//update contact handler
		@PostMapping("/process-update")
		public String updateHandler(@ModelAttribute Contact contact,
				@RequestParam("profileImage") MultipartFile file,
				Model model,
				HttpSession session,
				Principal principal) {
			
			try {
				
				//old contact details
				Contact oldContactDetail = contactRepositry.findById(contact.getcID()).get();
				
					if (!file.isEmpty()) {
						
						//file work
						//rewrite
						
						//delete old photo
						
						File deleteFile = new ClassPathResource("static/img").getFile();
						File file1 = new File(deleteFile,oldContactDetail.getImage());
						file1.delete();
						
						
						//update new photo
						
						File saveFile = new ClassPathResource("static/img").getFile();
						
						Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
						
						Files.copy(file.getInputStream(), path,StandardCopyOption.REPLACE_EXISTING);
						
						contact.setImage(file.getOriginalFilename());
					}else {
						
						contact.setImage(oldContactDetail.getImage());
						
					}
					
					User user = userRepository.getUserByUserName(principal.getName());
					
					contact.setUser(user);
					
					contactRepositry.save(contact);
					
					session.setAttribute("message",new Message("Your contact is updated..","success"));
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			System.out.println("contact Name"+contact.getName());
			System.out.println("contact Id"+contact.getcID());
			return "redirect:/user/"+contact.getcID()+"/contact";
		}
		
		//your profile handler
		@GetMapping("/profile")
		public String yourProfile(Model model) {
			
			 model.addAttribute("title", "profile page");
			 return "normal/profile";
		}
		
	
		////////////////////////
		@GetMapping("/delete-user/{id}")
		@Transactional
		public String deleteUser(@PathVariable("id") Integer id,
				Model model,
				HttpSession session,Principal principal) {
			
				userRepository.deleteById(id);
			
	
				session.setAttribute("message", new Message("User deleted succesfully...", "success"));
			
			return "redirect:/signup";
		}
		//////////////
		
		
		//opern user update form handler
		@PostMapping("/update-user/{id}")
		public String userupdateForm(@PathVariable("id") Integer id,Model model) {
			
			model.addAttribute("title", "Update User");
			
//			User user= userRepository.findById(id).get();
			
			User user = userRepository.findById(id).get();
			
			model.addAttribute("user", user);
		
//			model.addAttribute("contact", contact);
			
			return "normal/update_user";
			
		}
		
		
		@PostMapping("/do_update")
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
				
//				User result = this.userRepository.save(user); 
				
//				User userUpdate= userRepository.findById(id).get();
				
//				User updatedUser = userRepository.save(userUpdate);
//				Contact oldContactDetail = contactRepositry.findById(contact.getcID()).get();
				
				User olduserDetail = userRepository.findById(user.getId()).get();
				
				User updatedUser = userRepository.save(olduserDetail);
				
				model.addAttribute("user",new User());
				
				session.setAttribute("message", new Message("Successfully Updated User", "alert-success"));
				return "normal/update_user";
				
			} catch (Exception e) {
				e.printStackTrace();
				model.addAttribute("user", user);
				session.setAttribute("message", new Message("Somthing Went Wrong !!"+e.getMessage(), "alert-danger"));
				return "normal/update_user"; 
			}
			
			
		}
		
		
		
		
		
		
		
		
		
		
		
		
}
